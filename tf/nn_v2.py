import tensorflow as tf
import numpy as np
from pyexcel_ods import get_data, save_data
from collections import OrderedDict
from utils import export_model
from math import fabs

import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
model_name='nn_v2'

def normalize(data):
    min = data.min()
    max = data.max()
    for i in range(data.size):
        data[i] = (data[i] - min)/(max - min)
    return [data, min, max]

def roundup(data):
    for i in range(data.size):
        data[i] = round(data[i], 4)
    return data

def load_csv(fname, do_normalize=False, save_as_csv=False, split_percent=80):
    print('Loading data...', end='', flush=True)
    raw = np.array(get_data(fname)[fname][1:])
    x = raw[:,:5]
    y = raw[:,6:]
    print('done', flush=True)
    if do_normalize:
        print('Normalizing inputs...', end='', flush=True)
        coeffs = [[],[]]
        for i in range(x.shape[1]):
            [x[:,i], min, max] = normalize(x[:,i])
            coeffs[0].append(min)
            coeffs[1].append(max)
        coeffs = OrderedDict([('', coeffs)])
        save_data('normalize_{}.csv'.format(model_name), coeffs)
        for i in range(y.shape[1]):
            y[:,i] = roundup(y[:,i])
        if save_as_csv:
            cols = [['latitude', 'longitude', 'weekday', 'hour', 'min', 'green', 'orange', 'red', 'darkred']]
            rows = np.concatenate((x,y), axis=1)
            data = OrderedDict([('', np.concatenate((cols,rows), axis=0))])
            save_data('normalized_data.csv', data)
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
    return [training_set, test_set]

[training_set, test_set] = load_csv('v3.csv', do_normalize=True)
nTest = len(test_set['x'])
num_input = 5
num_targets = 3

n_hidden = 5

learning_rate = 0.2

num_steps=100
feature_columns = [tf.feature_column.numeric_column("x", shape=[num_input])]

regressor = tf.estimator.DNNRegressor(feature_columns=feature_columns,
                                        hidden_units=[n_hidden, n_hidden, n_hidden],
                                        label_dimension=num_targets,
                                        optimizer=tf.train.ProximalAdagradOptimizer(
                                            learning_rate=learning_rate),
                                        model_dir='meta/{}_model'.format(model_name))

train_input_fn = tf.estimator.inputs.numpy_input_fn(
    x={"x": np.array(training_set['x'])},
    y=np.array(training_set['y']),
    batch_size=len(training_set['x'])//4,
    num_epochs=num_steps,
    shuffle=True)
print('Training...', end='', flush=True)
# Train model.
regressor.train(input_fn=train_input_fn)
print('done')

# Define the test inputs
test_input_fn = tf.estimator.inputs.numpy_input_fn(
    x={"x": np.array(test_set['x'])},
    y=np.array(test_set['y']),
    num_epochs=1,
    shuffle=False)

print('Testing...', end='', flush=True)
# Evaluate accuracy.
accuracy_score = regressor.evaluate(input_fn=test_input_fn)
print('done')
print(accuracy_score)
samples = np.array(test_set['x'])
predict_input_fn = tf.estimator.inputs.numpy_input_fn(
    x={"x": samples},
    num_epochs=1,
    shuffle=False)

predictions = list(regressor.predict(input_fn=predict_input_fn))
error = np.zeros(num_targets)
for i in range(nTest):
    for j in range(num_targets):
        error[j] += fabs(round(predictions[i]['predictions'][j], 4) - test_set['y'][i][j]) * 100

avg = 0
for i in range(num_targets):
    error[i] /= nTest
    error[i] = round(error[i], 2)
    avg += error[i]
print("\nError rate for each output")
print(error)
print('Average error rate: {}%'.format(round(avg/num_targets, 2)))
export_model(regressor, model_name, num_input)
