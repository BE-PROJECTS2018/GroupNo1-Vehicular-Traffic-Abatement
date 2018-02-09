from utils import load_data
import tensorflow as tf

[training_set, test_set] = load_data('data')

num_inputs = 5
num_classes = 4

n_hidden1 = 3
n_hidden2 = 3

learning_rate = 0.1

# Specify that all features have real-value data
feature_columns = [tf.feature_column.numeric_column("x", shape=[num_input])]

dnn = tf.estimator.DNNClassifier(feature_columns=feature_columns,
                                hidden_units=[n_hidden1, n_hidden2],
                                n_classes=num_classes,
                                optimizer=tf.train.AdagradOptimizer(learning_rate=learning_rate),
                                model_dir='meta/nn_model')