from utils import load_data, export_model
import tensorflow as tf
import numpy as np
import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'

# from math import cos, sin
latmin = 19.039645023697126
latmax = 19.087125696060983
lngmin = 72.88591886230472
lngmax = 72.92025113769535

[training_set, test_set] = load_data('data')

num_input = 5
num_classes = 4

n_hidden = 10

learning_rate = 0.2

num_steps=100

# Specify that all features have real-value data
feature_columns = [tf.feature_column.numeric_column("x", shape=[num_input])]

classifier = tf.estimator.DNNClassifier(feature_columns=feature_columns,
                                hidden_units=[n_hidden, n_hidden, n_hidden],
                                n_classes=num_classes,
                                optimizer=tf.train.AdamOptimizer(learning_rate=learning_rate),
                                model_dir='meta/nn_model')

# Define the training inputs
train_input_fn = tf.estimator.inputs.numpy_input_fn(
    x={"x": np.array(training_set['x'])},
    y=np.array(training_set['y']),
    batch_size=len(training_set['x'])//8,
    num_epochs=num_steps,
    shuffle=True)
print('Training...', end='', flush=True)
# Train model.
classifier.train(input_fn=train_input_fn)
print('done')

test_input_fn = tf.estimator.inputs.numpy_input_fn(
    x={"x": np.array(training_set['x'])},
    y=np.array(training_set['y']),
    num_epochs=1,
    shuffle=False)

# Evaluate accuracy.
accuracy_score = classifier.evaluate(input_fn=test_input_fn)['accuracy']
#print(accuracy_score)

print("\nTrain Accuracy: {0:f}%\n".format(accuracy_score*100))

# Define the test inputs
test_input_fn = tf.estimator.inputs.numpy_input_fn(
    x={"x": np.array(test_set['x'])},
    y=np.array(test_set['y']),
    num_epochs=1,
    shuffle=False)

print('Testing...', end='', flush=True)
# Evaluate accuracy.
accuracy_score = classifier.evaluate(input_fn=test_input_fn)['accuracy']
#print(accuracy_score)
print('done')
print("\nTest Accuracy: {0:f}%\n".format(accuracy_score*100))

new_samples = np.array(
    [[(19.042930756538436-latmin)/(latmax-latmin), (72.89973760314945-lngmin)/(lngmax-lngmin), 4/6, 14/23, 50/59],
    [(19.053680376327897-latmin)/(latmax-latmin), (72.88806462951663-lngmin)/(lngmax-lngmin), 4/6, 14/23, 50/59]], dtype=np.float32)
predict_input_fn = tf.estimator.inputs.numpy_input_fn(
    x={"x": new_samples},
    num_epochs=1,
    shuffle=False)

predictions = list(classifier.predict(input_fn=predict_input_fn))
predicted_classes = [p["class_ids"] for p in predictions]
print(predicted_classes) # 3 2
# new_samples = np.array(training_set['x'], dtype=np.float32)
# predict_input_fn = tf.estimator.inputs.numpy_input_fn(
#     x={"x": new_samples},
#     num_epochs=1,
#     shuffle=False)

# predictions = list(classifier.predict(input_fn=predict_input_fn))
# acc = [predictions[i]["class_ids"][0] == training_set['y'][i] for i in range(len(predictions))]
# c = 0
# for a in acc:
#     if a:
#         c += 1
# print(c/len(acc)*100)
export_model(classifier, 'nn', num_input)