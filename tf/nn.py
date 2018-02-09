from utils import load_data
import tensorflow as tf
import numpy as np

[training_set, test_set] = load_data('data')

num_input = 5
num_classes = 4

n_hidden1 = 3
n_hidden2 = 3

learning_rate = 0.1

num_steps=100

# Specify that all features have real-value data
feature_columns = [tf.feature_column.numeric_column("x", shape=[num_input])]

classifier = tf.estimator.DNNClassifier(feature_columns=feature_columns,
                                hidden_units=[n_hidden1, n_hidden2],
                                n_classes=num_classes,
                                optimizer=tf.train.AdagradOptimizer(learning_rate=learning_rate),
                                model_dir='meta/nn_model')

# Define the training inputs
train_input_fn = tf.estimator.inputs.numpy_input_fn(
    x={"x": np.array(training_set['x'])},
    y=np.array(training_set['y']),
    num_epochs=None,
    shuffle=True)
print('Training...', end='')
# Train model.
classifier.train(input_fn=train_input_fn, steps=num_steps)
print('done')
# Define the test inputs
test_input_fn = tf.estimator.inputs.numpy_input_fn(
    x={"x": np.array(test_set['x'])},
    y=np.array(test_set['y']),
    num_epochs=1,
    shuffle=False)

# Evaluate accuracy.
accuracy_score = classifier.evaluate(input_fn=test_input_fn)
print(accuracy_score)

#print("\nTest Accuracy: {0:f}\n".format(accuracy_score))