import tensorflow as tf
import numpy as np
from pyexcel_ods import get_data
from utils import export_model
from math import fabs

import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
model_name='nn_v1'

def arrange(arr, ord):
    newarr = np.zeros(arr.shape)
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

[training_set, test_set, col_names] = load_csv('v1.csv')
batch_size=len(training_set['x']) // 2
counts = np.unique(training_set['y'], return_counts=True)[1]
counts.sort()
counts = counts[::-1]
ratios = np.zeros(counts.shape)
for i in range(counts.size):
    ratios[i] = counts[i]/sum(counts)
print('Ratios of classes', ratios*100)
ratios = 2 - ratios*2
print('Weights to penalize loss function', ratios)
trweights = np.zeros(len(training_set['x']))
for i in range(len(training_set['y'])):
    trweights[i] = ratios[training_set['y'][i]]

# weights = tf.constant(weights)
nTest = len(test_set['x'])
num_input = 5
num_classes = 4

n_hidden = 10

learning_rate = 0.2

num_steps=100

feature_columns = [tf.feature_column.numeric_column("x", shape=[num_input])]

def my_model(features, labels, mode, params):
    #Input layer
    net = tf.feature_column.input_layer(features, params['feature_columns'])
    
    #Prepare all hidden layers
    for units in params['hidden_units']:
        net = tf.layers.dense(net, units=units, activation=tf.sigmoid)
    
    # Compute logits (1 per class).
    logits = tf.layers.dense(net, params['n_classes'], activation=None)
    # Compute predictions.
    predicted_classes = tf.argmax(logits, 1)
    if mode == tf.estimator.ModeKeys.PREDICT:
        predictions = {
            'class_ids': predicted_classes,
            'probabilities': tf.nn.softmax(logits),
            'logits': logits,
        }
        return tf.estimator.EstimatorSpec(mode, predictions=predictions)
    
    Tweights = tf.constant(trweights)
    # Compute loss.
    loss = tf.losses.sparse_softmax_cross_entropy(labels=labels, logits=logits, weights=Tweights)
    
    # class_weight = tf.constant([ratios])
    # weight_per_label = tf.transpose( tf.matmul(labels32
    #                        , tf.transpose(class_weight)) )

    # xent = tf.mul(weight_per_label
    #      , tf.nn.sparse_softmax_cross_entropy_with_logits(logits, labels, name="xent_raw")) #shape [1, batch_size]
    # loss = tf.reduce_mean(xent) #shape 1
    # Compute evaluation metrics.
    accuracy = tf.metrics.accuracy(labels=labels,
                                   predictions=predicted_classes,
                                   name='acc_op')
    metrics = {'accuracy': accuracy}
    tf.summary.scalar('accuracy', accuracy[1])

    if mode == tf.estimator.ModeKeys.EVAL:
        return tf.estimator.EstimatorSpec(
            mode, loss=loss, eval_metric_ops=metrics)

    # Create training op.
    assert mode == tf.estimator.ModeKeys.TRAIN

    optimizer = tf.train.AdagradOptimizer(learning_rate=learning_rate)
    train_op = optimizer.minimize(loss, global_step=tf.train.get_global_step())
    return tf.estimator.EstimatorSpec(mode, loss=loss, train_op=train_op)

# classifier = tf.estimator.Estimator(
#     model_dir='./meta/{}_model'.format(model_name),
#     model_fn=my_model,
#     params={
#         'feature_columns': feature_columns,
#         'hidden_units': [n_hidden, n_hidden],
#         'n_classes': num_classes,
#     })

classifier = tf.estimator.DNNClassifier(feature_columns=feature_columns,
                                hidden_units=[n_hidden, n_hidden, n_hidden],
                                n_classes=num_classes,
                                weight_column='weight',
                                optimizer=tf.train.AdagradOptimizer(learning_rate=learning_rate),
                                model_dir='./meta/{}_model'.format(model_name))

train_input_fn = tf.estimator.inputs.numpy_input_fn(
    x={"x": np.array(training_set['x']), 'weight': trweights},
    y=np.array(training_set['y']),
    batch_size=batch_size,
    num_epochs=num_steps,
    shuffle=True)

print('Training...', end='', flush=True)
# Train model.
classifier.train(input_fn=train_input_fn)


test_input_fn = tf.estimator.inputs.numpy_input_fn(
    x={"x": np.array(training_set['x']), 'weight': trweights},
    y=np.array(training_set['y']),
    batch_size=batch_size,
    num_epochs=1,
    shuffle=False)

# Evaluate accuracy.
accuracy_score = classifier.evaluate(input_fn=test_input_fn)#['accuracy']
print('done')
print(accuracy_score)
# print("\nTrain Accuracy: {0:f}%\n".format(accuracy_score*100))

new_samples = np.array(training_set['x'], dtype=np.float32)
predict_input_fn = tf.estimator.inputs.numpy_input_fn(
    x={"x": new_samples},
    num_epochs=1,
    shuffle=False)

predictions = list(classifier.predict(input_fn=predict_input_fn))
predictions = [predictions[i]["class_ids"][0]for i in range(len(predictions))]
counts = np.unique(predictions, return_counts=True)
counts = arrange(counts[1], counts[0])
ratios = np.zeros(counts.shape)
for i in range(counts.size):
    ratios[i] = counts[i]/sum(counts)
print('Ratios of predicted classes', ratios*100)

counts = np.unique(test_set['y'], return_counts=True)[1]
counts.sort()
counts = counts[::-1]
ratios = np.zeros(counts.shape)
for i in range(counts.size):
    ratios[i] = counts[i]/sum(counts)
ratios = 2 - ratios*2
teweights = np.zeros(len(test_set['x']))
for i in range(len(test_set['y'])):
    teweights[i] = ratios[test_set['y'][i]]

# Define the test inputs
test_input_fn = tf.estimator.inputs.numpy_input_fn(
    x={"x": np.array(test_set['x']), 'weight': teweights},
    y=np.array(test_set['y']),
    num_epochs=1,
    shuffle=False)

print('Testing...', end='', flush=True)
# Evaluate accuracy.
accuracy_score = classifier.evaluate(input_fn=test_input_fn)
print('done')
print(accuracy_score)
# print("\nTest Accuracy: {0:f}%\n".format(accuracy_score*100))
export_model(classifier, model_name, num_input)