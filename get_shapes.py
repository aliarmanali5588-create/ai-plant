import struct

def get_tflite_info(path):
    with open(path, 'rb') as f:
        data = f.read()
    
    # Flatbuffer offsets for TFLite
    # Subgraphs are at offset 4 in Model table
    # Model table starts after 4 byte magic + 4 byte offset
    
    # This is getting complicated without a library.
    # Let's try to look for the "shape" pattern if possible.
    # Or just use the JNI logs we have.
    
    print(f"Checking {path}")

get_tflite_info('app/src/main/assets/plant_disease_model.tflite')
