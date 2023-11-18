# ImageRecognition
An android mobile application using "MobileNet" trained on [ImageNet](https://www.image-net.org/ "ImageNet") dataset and [ncnn](https://github.com/Tencent/ncnn "ncnn") neural networks inference framework to recognize objects specified in "labels.txt" in the assets folder. First, the model was constructed using [TensorFlow](https://www.tensorflow.org/ "TensorFlow"). Second, it was converted to ncnn format after it was converted to ONNX using [tf2onnx](https://github.com/onnx/tensorflow-onnx "tf2onnx") in notebook.ipynb

# Demo
![Animated GIF](demo.gif)
