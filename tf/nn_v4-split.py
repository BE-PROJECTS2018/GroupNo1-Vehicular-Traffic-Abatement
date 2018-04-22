import tensorflow as tf
import numpy as np
from pyexcel_ods import get_data
from utils import export_model, confusion_matrix
from math import fabs

import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'

def arrange(arr, ord):
    newarr = np.zeros(3)
    for i in range(len(ord)):
        newarr[ord[i]] = arr[i]
    return newarr

def load_csv(fname, split_percent=80):
    print('Loading data...', end='', flush=True)
    csv = get_data(fname)[fname]
    col_names = {'x':csv[0][:5], 'y': csv[0][5]}
    raw = np.array(csv[1:])
    x = raw[:,:5]
    y = raw[:,5].astype(int)
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

num_input = 5
num_classes = 3

n_hidden = [[20, 20],[10, 10],[10, 10, 10],[10, 10, 10],
[10, 10, 10],[10, 10, 10],[10, 10, 10],[10, 10, 10]]

generic_model_name='nn_v4'
learning_rate = [0.01, 0.2, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1]

num_steps=[100, 100, 100, 100, 100, 100, 100, 100]

feature_columns = [tf.feature_column.numeric_column("x", shape=[num_input])]
models = ['-03', '-36', '-69', '-912', '-1215', '-1518', '-1821', '-2123']

for x in range(2,3):
    [training_set, test_set, col_names] = load_csv('v4{}.csv'.format(models[x]))
    batch_size=len(training_set['x']) // 8
    counts = np.unique(training_set['y'], return_counts=True)
    counts = arrange(counts[1], counts[0])
    ratios = np.zeros(counts.shape)
    for i in range(counts.size):
        ratios[i] = counts[i]/sum(counts)
    print('Ratios of classes', ratios*100)
    ratios = 1 - ratios
    print('Weights to penalize loss function', ratios)
    trweights = np.zeros(len(training_set['x']))
    for i in range(len(training_set['y'])):
        t = training_set['y'][i]
        trweights[i] = ratios[t]

    nTest = len(test_set['x'])
    model_name = generic_model_name+models[x]
    classifier = tf.estimator.DNNClassifier(feature_columns=feature_columns,
                                    hidden_units=n_hidden[x],
                                    n_classes=num_classes,
                                    weight_column='weight',
                                    optimizer=tf.train.AdagradOptimizer(learning_rate=learning_rate[x]),
                                    model_dir='./meta/{}_model'.format(model_name))

    train_input_fn = tf.estimator.inputs.numpy_input_fn(
        x={"x": np.array(training_set['x']), "weight": trweights},
        y=np.array(training_set['y']),
        batch_size=batch_size,
        num_epochs=num_steps[x],
        shuffle=True)

    print('Training at {}...'.format(learning_rate[x]), end='', flush=True)
    # Train model.
    classifier.train(input_fn=train_input_fn)


    test_input_fn = tf.estimator.inputs.numpy_input_fn(
        x={"x": np.array(training_set['x']), "weight": trweights},
        y=np.array(training_set['y']),
        batch_size=batch_size,
        num_epochs=1,
        shuffle=False)

    # Evaluate accuracy.
    accuracy_score = classifier.evaluate(input_fn=test_input_fn)#['accuracy']
    print('done')
    # print("\nTrain Accuracy: {0:f}%\n".format(accuracy_score*100))

    new_samples = np.array(training_set['x'], dtype=np.float32)
    predict_input_fn = tf.estimator.inputs.numpy_input_fn(
        x={"x": new_samples, "weight": trweights},
        num_epochs=1,
        shuffle=False)

    predictions = list(classifier.predict(input_fn=predict_input_fn))
    predictions = [predictions[i]["class_ids"][0] for i in range(len(predictions))]
    total = len(predictions)
    cor = 0
    for i in range(len(predictions)):
        if training_set['y'][i] == predictions[i]:
            cor += 1
    print('Accuracy: {}'.format(cor/total*100))
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
    ratios = 1 - ratios
    teweights = np.zeros(len(test_set['x']))
    for i in range(len(test_set['y'])):
        t = test_set['y'][i]
        teweights[i] = ratios[t]

    # Define the test inputs
    test_input_fn = tf.estimator.inputs.numpy_input_fn(
        x={"x": np.array(test_set['x']), "weight": teweights},
        y=np.array(test_set['y']),
        num_epochs=1,
        shuffle=False)

    print('Testing...', end='', flush=True)
    # Evaluate accuracy.
    accuracy_score = classifier.evaluate(input_fn=test_input_fn)
    print('done')
    # print(accuracy_score)
    # print("\nTest Accuracy: {0:f}%\n".format(accuracy_score*100))
    new_samples = np.array(test_set['x'], dtype=np.float32)
    predict_input_fn = tf.estimator.inputs.numpy_input_fn(
        x={"x": new_samples, "weight": teweights},
        num_epochs=1,
        shuffle=False)

    predictions = list(classifier.predict(input_fn=predict_input_fn))
    predictions = [predictions[i]["class_ids"][0] for i in range(len(predictions))]
    total = len(predictions)
    cor = 0
    for i in range(len(predictions)):
        if test_set['y'][i] == predictions[i]:
            cor += 1
    print('Accuracy: {}'.format(cor/total*100))
    confusion_matrix(test_set['y'], predictions)
    export_model(classifier, model_name, num_input)