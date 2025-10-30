from flask import Flask, request, jsonify
import joblib
import pandas as pd
import re
import numpy as np
from datetime import datetime

app = Flask(__name__)
CURRENT_YEAR = datetime.now().year

# ⭐ ĐÃ SỬA: Ánh xạ 2 mô hình CAR mới

MODEL_MAP = {
    'bike': 'pricing_model_bike.pkl',
    'motorbike': 'pricing_model_motorbike.pkl',
    # ⭐ ĐÃ GỘP: 2 sub-segments cho CAR
    'car_low': 'pricing_model_car_low.pkl',
    'car_high': 'pricing_model_car_high.pkl', 
    
    'battery': 'pricing_model_battery.pkl', 
    'other': 'pricing_model_other.pkl',
    'missing': 'pricing_model_other.pkl',
}

LOADED_MODELS = {}

# --- Dữ liệu Mean Encoding (Giữ nguyên, cần cập nhật lại sau khi train) ---
GLOBAL_BRAND_MEAN_LOG_PRICE = 18.10701871833586 
BRAND_MEAN_LOG_PRICES = {
    'Ado': 15.693924155104074, 'Asama': 15.566611387648745, 'Audi': 20.36949746138378, 'BMW': 20.96284874570941, 'BYD': 18.885734945275573, 'CALB': 17.20514810640709, 'CATL': 17.258820534464625, 'DKBike': 17.142810444704818, 'Dat Bike': 17.382610550097446, 'Dibao': 16.801406339263472, 'Engwe': 16.03644994630763, 'Eve Energy': 17.22861938142199, 'Giant': 16.01571338996727, 'Gogoro': 16.784698143033715, 'Gotion': 17.219541647258488, 'Gotion High-Tech': 18.028128926938688, 'Himo': 16.113998553160574, 'Honda': 19.810690081345292, 'Hyundai': 20.248506822512162, 'Kia': 20.072135262785356, 'LG Chem': 17.22153903877945, 'Lishen': 16.700635642284034, 'MG': 19.3265542397311, 'Mercedes': 21.06058025428384, 'Nissan': 19.46970095665683, 'Niu': 16.915823100200424, 'Panasonic': 17.21659759606634, 'Pega': 17.10159037922259, 'Phylion': 16.44264006813076, 'Porsche': 20.302818125880172, 'SK On': 17.596106949573056, 'Samsung': 17.49560067924235, 'Samsung SDI': 17.674055228765738, 'Specialized': 16.609769705896483, 'Tesla': 20.352466279153568, 'Trek': 16.577936508904923, 'Vinfast': 19.139803009744618, 'Vinfast Klara': 17.07699451311597, 'Wuling': 19.894476872100956, 'Xiaomi': 16.2161119184011, 'Yadea': 17.18303850026512, 'Zero': 17.486405551979658, 'missing': 18.028128926938688
}

# Ngưỡng Giá Logarithmic Dựa trên Brand Value Score
CAR_LOG_THRESHOLD_LOW = 20.21 # Tương đương 600M

# --- Hàm xử lý dữ liệu (Đồng bộ với train_model.py) ---
def extract_capacity_value(capacity_str):
    if pd.isna(capacity_str): return 0
    capacity_str = str(capacity_str).lower().replace(" ", "")
    match = re.search(r'(\d+(\.\d+)?)', capacity_str)
    if not match: return 0
    value = float(match.group(1))
    if 'kwh' in capacity_str: return value
    if 'ah' in capacity_str:
        return (value * 48) / 1000 if value > 100 else (value * 72) / 1000
    return value

def extract_lifespan_months(lifespan_str):
    if pd.isna(lifespan_str): return 0
    match = re.search(r'(\d+)', str(lifespan_str))
    return int(match.group(1)) if match else 0

def extract_charge_time(time_str):
    if pd.isna(time_str): return 0
    numbers = re.findall(r'\d+(?:\.\d+)?', str(time_str))
    valid_numbers = [float(n) for n in numbers if n.strip()]
    return max(valid_numbers) if valid_numbers else 0 

def calculate_wear_score(row):
    if row.get("productType") in ["bike", "motorbike"]:
        mileage = max(row.get('mileage', 0), 0)
        cycles = max(row.get('chargeCycles', 0), 0)
        mileage_factor = 1 / np.log1p(mileage) if mileage > 0 else 1
        cycles_factor = 1 / np.log1p(cycles) if cycles > 0 else 1
        return (mileage_factor * 0.6 + cycles_factor * 0.4)
    return 1.0 


# --- Khởi tạo & Tải các mô hình ---
def load_models():
    print("Đang tải các mô hình chuyên biệt...")
    success = True
    for filename in set(MODEL_MAP.values()):
        if filename not in LOADED_MODELS:
            try:
                LOADED_MODELS[filename] = joblib.load(filename)
                print(f"✅ Tải mô hình {filename} thành công.")
            except FileNotFoundError:
                print(f"❌ Không tìm thấy {filename}. Hãy chạy train_model.py trước.")
                success = False
    return success

if not load_models():
    print("Cần có các file mô hình để chạy API.")
    
# --- API chính ---
@app.route('/predict', methods=['POST'])
def predict_price():
    data = request.get_json()
    if not data:
        return jsonify({'error': 'Không nhận được dữ liệu JSON.'}), 400
        
    product_type = str(data.get('productType', 'missing')).lower()
    
    # 1. Logic chọn mô hình dựa trên productType
    if product_type == 'car':
        brand = str(data.get('brand', 'missing'))
        brand_score = BRAND_MEAN_LOG_PRICES.get(brand, GLOBAL_BRAND_MEAN_LOG_PRICE)
        
        if brand_score <= CAR_LOG_THRESHOLD_LOW: 
            model_filename = MODEL_MAP['car_low']
        else:
            model_filename = MODEL_MAP['car_high']
    else:
        model_filename = MODEL_MAP.get(product_type, MODEL_MAP['missing'])
        
    model = LOADED_MODELS.get(model_filename)

    if model is None:
        return jsonify({'error': f'Mô hình {model_filename} chưa được tải.'}), 500

    try:
        # 2. Trích xuất đặc trưng (Đồng bộ với train_model.py)
        data['productType'] = product_type
        data['batteryCapacity_numeric'] = extract_capacity_value(data.get('batteryCapacity'))
        data['batteryLifespan_months'] = extract_lifespan_months(data.get('batteryLifespan'))
        data['chargeTime_numeric'] = extract_charge_time(data.get('chargeTime'))

        condition_id = data.get('conditionId')
        data['condition_score'] = (5 - condition_id) if condition_id in [1, 2, 3, 4] else 0

        year = data.get('yearOfManufacture')
        data['yearOfManufacture_numeric'] = pd.to_numeric(year, errors="coerce")
        data['age'] = (CURRENT_YEAR - data['yearOfManufacture_numeric']) if (pd.notna(data['yearOfManufacture_numeric']) and data['yearOfManufacture_numeric'] > 1900) else 0 

        input_df = pd.DataFrame([data])
        
        if 'mileage' not in input_df.columns: input_df['mileage'] = 0
        if 'chargeCycles' not in input_df.columns: input_df['chargeCycles'] = 0

        input_df['wear_score'] = input_df.apply(calculate_wear_score, axis=1)

        DEFAULT_MAX_SPEED = 50 
        if 'maxSpeed' not in input_df.columns:
            input_df['maxSpeed'] = DEFAULT_MAX_SPEED
        else:
            input_df['maxSpeed'] = pd.to_numeric(input_df['maxSpeed'], errors='coerce').fillna(DEFAULT_MAX_SPEED)

        input_df['maxSpeed_safe'] = input_df['maxSpeed'].replace(0, 1e-6) 
        input_df["pin_value_per_speed"] = input_df["batteryCapacity_numeric"] / input_df['maxSpeed_safe']
        
        if 'brand' not in input_df.columns:
            input_df['brand'] = 'missing'
            
        input_df['brand'] = input_df['brand'].fillna("missing")
        input_df['brand_value_score'] = input_df['brand'].apply(
            lambda x: BRAND_MEAN_LOG_PRICES.get(x, GLOBAL_BRAND_MEAN_LOG_PRICE)
        )

        # 3. Dự đoán
        prediction = model.predict(input_df)
        predicted_price = np.expm1(prediction[0]) # Đây là numpy.float32

        # 4. Tránh giá âm (Đã bỏ làm tròn)
        MINIMUM_PRICE = 200_000
        final_price = max(MINIMUM_PRICE, predicted_price) # Đây CŨNG là numpy.float32


        # ⭐⭐⭐ SỬA LỖI THEO YÊU CẦU: Chuyển suggestedPrice sang int (số nguyên) ⭐⭐⭐
        return jsonify({
            'suggestedPrice': int(final_price), # <--- SỬA LỖI TẠI ĐÂY
            'model_used': model_filename,
            'model_raw_prediction': float(predicted_price), # Giữ raw prediction là float để debug
            'model_version': f'XGBoost_Segmented_v7.1_{CURRENT_YEAR}' 
        })

    except Exception as e:
        print(f"❌ Lỗi xử lý request: {e}")
        import traceback
        traceback.print_exc()
        return jsonify({'error': f'Lỗi server nội bộ: {str(e)}'}), 500

# --- Chạy ứng dụng ---
if __name__ == '__main__':
    print(f"🚀 API đang chạy tại http://127.0.0.1:5000 (năm {CURRENT_YEAR})")
    app.run(host='0.0.0.0', port=5000, debug=True)