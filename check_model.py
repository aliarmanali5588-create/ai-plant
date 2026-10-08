import struct

def get_tflite_metadata(path):
    with open(path, 'rb') as f:
        data = f.read()
    
    # Very simple TFLite parser for metadata
    # This is a bit complex without flatbuffers, but we can look for "TFL3" identifier
    # and try to find the tensors.
    
    print(f"File size: {len(data)} bytes")
    if data[4:8] != b'TFL3':
        print("Not a valid TFLite file (missing TFL3 marker)")
        return

    # Let's try to find common strings or patterns if we can't parse easily
    # Actually, let's just use the JNI logs we added to TFLiteVisionProvider
    # [PC][5] Model Input: shape=[1, 224, 224, 3], type=FLOAT32
    # The user says the report said "4x224x224x3 allocated".
    # 4 is the byte size of Float32.
    
get_tflite_metadata('app/src/main/assets/plant_disease_model.tflite')
