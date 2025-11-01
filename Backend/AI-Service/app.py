# File: mq_consumer.py
# (File này sẽ thay thế app.py)

import pika
import json
import joblib
import pandas as pd
import numpy as np
import sys

# --- TẢI MODEL (Giống hệt app.py) ---
print("Loading pricing models...")
try:
    models = {
        "car_low": joblib.load('pricing_model_car_low.pkl'),
        "car_high": joblib.load('pricing_model_car_high.pkl'),
        "motorbike": joblib.load('pricing_model_motorbike.pkl'),
        "bike": joblib.load('pricing_model_bike.pkl'),
        "battery": joblib.load('pricing_model_battery.pkl')
    }
    print("Models loaded successfully.")
except Exception as e:
    print(f"Error loading models: {e}")
    sys.exit(1) # Dừng nếu không tải được model

# Định nghĩa tên Queue (phải khớp với Java)
AI_REQUEST_QUEUE = 'ai.price.request.queue'

# --- HÀM DỰ ĐOÁN (Giống hệt app.py) ---
def predict_price(data):
    product_type = data.get('productType')
    
    if product_type == 'car':
        model_low = models['car_low']
        model_high = models['car_high']
        
        features_low = pd.DataFrame([{
            'Year': data.get('yearOfManufacture', 2020),
            'Mileage': data.get('mileage', 0),
            'ConditionID': data.get('conditionId', 3)
        }])
        
        features_high = pd.DataFrame([{
            'Brand': data.get('brand', 'Other'),
            'MaxSpeed': data.get('maxSpeed', 0),
            'Range': data.get('rangePerCharge', 0),
            'Warranty': data.get('warrantyPolicy', 'None')
        }])
        
        price_low = model_low.predict(features_low)[0]
        price_high = model_high.predict(features_high)[0]
        predicted_price = (price_low + price_high) / 2
        
    elif product_type in ['motorbike', 'bike', 'battery']:
        model = models[product_type]
        
        features_dict = {
            'Year': data.get('yearOfManufacture', 2020),
            'ConditionID': data.get('conditionId', 3),
            'Warranty': data.get('warrantyPolicy', 'None')
        }
        
        if product_type == 'motorbike':
            features_dict['Range'] = data.get('rangePerCharge', 0)
            features_dict['MaxSpeed'] = data.get('maxSpeed', 0)
        elif product_type == 'battery':
            features_dict['Capacity'] = data.get('batteryCapacity', '0kWh')
            features_dict['Type'] = data.get('batteryType', 'Other')
            features_dict['Cycles'] = data.get('chargeCycles', 0)

        features = pd.DataFrame([features_dict])
        predicted_price = model.predict(features)[0]
        
    else:
        return 200000  # Giá mặc định nếu loại không xác định

    # Làm tròn và đảm bảo giá tối thiểu
    final_price = max(200000, round(predicted_price / 1000) * 1000)
    return int(final_price)


# --- HÀM XỬ LÝ KHI NHẬN TIN NHẮN ---
def on_request(ch, method, properties, body):
    try:
        # 1. Nhận dữ liệu (dạng JSON string)
        request_data = json.loads(body.decode('utf-8'))
        print(f" [.] Received request: {request_data}")

        # 2. Gọi hàm dự đoán
        suggested_price = predict_price(request_data)
        response_data = {'suggestedPrice': suggested_price}
        print(f" [.] Predicted price: {suggested_price}")

    except Exception as e:
        print(f" [!] Error processing request: {e}")
        response_data = {'suggestedPrice': 400000} # Giá mặc định nếu lỗi

    # 3. Gửi Phản hồi (Reply)
    # Gửi kết quả về 'reply_to' queue
    # với 'correlation_id' giống hệt tin nhắn gốc
    ch.basic_publish(
        exchange='',
        routing_key=properties.reply_to,
        properties=pika.BasicProperties(correlation_id=properties.correlation_id),
        body=json.dumps(response_data)
    )
    
    # 4. Báo cho RabbitMQ biết là đã xử lý xong tin nhắn này
    ch.basic_ack(delivery_tag=method.delivery_tag)

# --- THIẾT LẬP KẾT NỐI VÀ LẮNG NGHE ---
try:
    connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
    channel = connection.channel()

    # Khai báo Queue (nếu chưa có)
    channel.queue_declare(queue=AI_REQUEST_QUEUE)

    # Đặt service ở chế độ "nhận 1 tin nhắn mỗi lần" để cân bằng tải
    channel.basic_qos(prefetch_count=1)
    
    # Đặt hàm on_request làm callback khi có tin nhắn
    channel.basic_consume(queue=AI_REQUEST_QUEUE, on_message_callback=on_request)

    print(f" [x] Awaiting RPC requests on '{AI_REQUEST_QUEUE}'")
    channel.start_consuming()

except pika.exceptions.AMQPConnectionError as e:
    print(f"Error connecting to RabbitMQ: {e}")
    print("Please ensure RabbitMQ is running on localhost:5672")
except KeyboardInterrupt:
    print('Interrupted')
    connection.close()