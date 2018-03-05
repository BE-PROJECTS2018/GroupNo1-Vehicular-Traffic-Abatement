import tensorflow as tf
import numpy as np
from pyexcel_ods import get_data
from utils import export_model, confusion_matrix
from math import fabs

import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
model_name='nn_v1-1-geohash'

def arrange(arr, ord):
    newarr = np.zeros(4)
    for i in range(len(ord)):
        newarr[ord[i]] = arr[i]
    return newarr

def load_csv(fname, split_percent=80):
    print('Loading data...', end='', flush=True)
    csv = get_data(fname)[fname]
    col_names = {'x':csv[0][:-1], 'y': csv[0][-1]}
    raw = np.array(csv[1:])
    x = raw[:,:-1]
    y = raw[:,-1].astype(int)
    print('done', flush=True)
    total = len(x)
    n_train = (int)(total*(split_percent/100))
    training_set = {'x': [], 'y': []}
    test_set = {'x': [], 'y': []}
    print('Splitting into training and test set at {}%...'.format(split_percent), end='', flush=True)
    for i in range(n_train):
        training_set['x'].append(x[i])
        training_set['y'].append(y[i])
    for i in range(n_train,total):
        test_set['x'].append(x[i])
        test_set['y'].append(y[i])
    print('done', flush=True)
    return [training_set, test_set, col_names]

[training_set, test_set, col_names] = load_csv('v1-1-geohash.csv')
batch_size=len(training_set['x']) // 2
counts = np.unique(training_set['y'], return_counts=True)
counts = arrange(counts[1], counts[0])
ratios = np.zeros(counts.shape)
for i in range(counts.size):
    ratios[i] = counts[i]/sum(counts)
print('Ratios of classes', ratios*100)
m = max(counts)
for i in range(counts.size):
    ratios[i] = m/counts[i]
print('Weights to penalize loss function', ratios)
trweights = np.zeros(len(training_set['x']))
for i in range(len(training_set['y'])):
    trweights[i] = ratios[training_set['y'][i]]

# weights = tf.constant(weights)
nTest = len(test_set['x'])
num_input = 4
num_classes = 4

n_hidden = 10

learning_rate = 0.2

num_steps=100

feature_columns = [
    tf.feature_column.embedding_column(tf.feature_column.categorical_column_with_hash_bucket('geohash', hash_bucket_size=44137), dimension=int(44137**0.25)),
    tf.feature_column.numeric_column('weekday'),
    tf.feature_column.numeric_column('hours'),
    tf.feature_column.numeric_column('minutes')
    ]

classifier = tf.estimator.DNNClassifier(feature_columns=feature_columns,
                                hidden_units=[n_hidden, n_hidden, n_hidden],
                                n_classes=num_classes,
                                weight_column='weight',
                                optimizer=tf.train.AdagradOptimizer(learning_rate=learning_rate),
                                model_dir='./meta/{}_model'.format(model_name))

train_input_fn = tf.estimator.inputs.numpy_input_fn(
    x={"geohash": np.array(training_set['x'])[:,0],
    "weekday": np.array(training_set['x'])[:,1].astype(int),
    "hours": np.array(training_set['x'])[:,2].astype(int),
    "minutes": np.array(training_set['x'])[:,3].astype(int),
    'weight': trweights},
    y=np.array(training_set['y']),
    batch_size=batch_size,
    num_epochs=num_steps,
    shuffle=True)

print('Training...', end='', flush=True)
# Train model.
classifier.train(input_fn=train_input_fn)


test_input_fn = tf.estimator.inputs.numpy_input_fn(
    x={"geohash": np.array(training_set['x'])[:,0],
    "weekday": np.array(training_set['x'])[:,1].astype(int),
    "hours": np.array(training_set['x'])[:,2].astype(int),
    "minutes": np.array(training_set['x'])[:,3].astype(int),
    'weight': trweights},
    y=np.array(training_set['y']),
    batch_size=batch_size,
    num_epochs=1,
    shuffle=False)

# Evaluate accuracy.
accuracy_score = classifier.evaluate(input_fn=test_input_fn)#['accuracy']
print('done')
print(accuracy_score)
# print("\nTrain Accuracy: {0:f}%\n".format(accuracy_score*100))

new_samples = np.array(training_set['x'])
predict_input_fn = tf.estimator.inputs.numpy_input_fn(
    x={"geohash": new_samples[:,0],
    "weekday": new_samples[:,1].astype(int),
    "hours": new_samples[:,2].astype(int),
    "minutes": new_samples[:,3].astype(int),
    'weight': trweights},
    num_epochs=1,
    shuffle=False)

predictions = list(classifier.predict(input_fn=predict_input_fn))
predictions = [predictions[i]["class_ids"][0] for i in range(len(predictions))]
confusion_matrix(training_set['y'], predictions)
counts = np.unique(predictions, return_counts=True)
counts = arrange(counts[1], counts[0])
ratios = np.zeros(counts.shape)
for i in range(counts.size):
    ratios[i] = counts[i]/sum(counts)
print('Ratios of predicted classes', ratios*100)

counts = np.unique(test_set['y'], return_counts=True)
counts = arrange(counts[1], counts[0])
ratios = np.zeros(counts.shape)
for i in range(counts.size):
    ratios[i] = counts[i]/sum(counts)
print('Ratios of test set classes', ratios)
m = max(counts)
for i in range(counts.size):
    ratios[i] = m/counts[i]
teweights = np.zeros(len(test_set['x']))
for i in range(len(test_set['y'])):
    teweights[i] = ratios[test_set['y'][i]]

# Define the test inputs
test_input_fn = tf.estimator.inputs.numpy_input_fn(
    x={"geohash": np.array(test_set['x'])[:,0],
    "weekday": np.array(test_set['x'])[:,1].astype(int),
    "hours": np.array(test_set['x'])[:,2].astype(int),
    "minutes": np.array(test_set['x'])[:,3].astype(int),
    'weight': teweights},
    y=np.array(test_set['y']),
    num_epochs=1,
    shuffle=False)

print('Testing...', end='', flush=True)
# Evaluate accuracy.
accuracy_score = classifier.evaluate(input_fn=test_input_fn)
print('done')
print(accuracy_score)
new_samples = np.array(test_set['x'])
predict_input_fn = tf.estimator.inputs.numpy_input_fn(
    x={"geohash": new_samples[:,0],
    "weekday": new_samples[:,1].astype(int),
    "hours": new_samples[:,2].astype(int),
    "minutes": new_samples[:,3].astype(int),
    'weight': teweights},
    num_epochs=1,
    shuffle=False)

predictions = list(classifier.predict(input_fn=predict_input_fn))
predictions = [predictions[i]["class_ids"][0] for i in range(len(predictions))]
confusion_matrix(test_set['y'], predictions)
counts = np.unique(predictions, return_counts=True)
counts = arrange(counts[1], counts[0])
ratios = np.zeros(counts.shape)
for i in range(counts.size):
    ratios[i] = counts[i]/sum(counts)
print('Ratios of predicted classes', ratios*100)
# print("\nTest Accuracy: {0:f}%\n".format(accuracy_score*100))
print('Exporting model...', end='', flush=True)
# Export trained model
def serving_input_receiver_fn():
    inputs = {"geohash": tf.placeholder(shape=[None, 1], dtype=tf.string),
    "weekday": tf.placeholder(shape=[None, 1], dtype=tf.float32),
    "hours": tf.placeholder(shape=[None, 1], dtype=tf.float32),
    "minutes": tf.placeholder(shape=[None, 1], dtype=tf.float32)}
    return tf.estimator.export.ServingInputReceiver(inputs, inputs)
export_dir='meta/{}_export'.format(model_name)
classifier.export_savedmodel(export_dir_base=export_dir, serving_input_receiver_fn=serving_input_receiver_fn)
print('done')