# convert_onnx_to_tflite.py
import onnx
import tensorflow as tf

# ─── Monkey‐patch missing ops so onnx-tf doesn’t crash ────────────────────────
# onnx-tf 1.5.0 expects tf.ceil, tf.floor, etc. which moved under tf.math
tf.ceil  = tf.math.ceil
tf.floor = tf.math.floor
tf.abs   = tf.math.abs
tf.sin   = tf.math.sin
tf.cos   = tf.math.cos
# (add any other tf.<op> the traceback complains about)

# ─── Now import onnx-tf backend ───────────────────────────────────────────────
from onnx_tf.backend import prepare
import os

# 1) Load the ONNX model
onnx_model = onnx.load("app/src/main/cpp/llama.cpp/build/gemma3nlu.onnx")

# 2) Convert ONNX → TensorFlow SavedModel
tf_rep = prepare(onnx_model, strict=False)
export_dir = "app/src/main/assets/models/gemma3nlu_saved_model"
if os.path.exists(export_dir):
    tf.io.gfile.rmtree(export_dir)
tf_rep.export_graph(export_dir)

# 3) Convert SavedModel → TFLite
converter = tf.lite.TFLiteConverter.from_saved_model(export_dir)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

# 4) Write out the .tflite file
out_path = "app/src/main/assets/models/gemma3nlu.tflite"
with open(out_path, "wb") as f:
    f.write(tflite_model)

print("✅ Wrote TFLite model to", out_path)
