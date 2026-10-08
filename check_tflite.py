import struct

def check_tflite(path):
    try:
        with open(path, 'rb') as f:
            data = f.read()
        
        print(f"File: {path}")
        print(f"Size: {len(data)} bytes")
        
        if len(data) < 8:
            print("File too small")
            return

        # Check for TFLITE identifier in the header
        # Usually at offset 4
        magic = data[4:8]
        print(f"Magic at 4-7: {magic}")
        
        if magic != b'TFL3':
            print("Warning: Magic 'TFL3' not found at offset 4.")
            
        # Try to find "main" or other strings
        if b'input' in data:
            print("Found 'input' string in file")
        if b'output' in data:
            print("Found 'output' string in file")
            
    except Exception as e:
        print(f"Error: {e}")

check_tflite('app/src/main/assets/plant_disease_model.tflite')
