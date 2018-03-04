import json, glob
from random import shuffle
import tensorflow as tf
from math import cos, sin
import numpy as np
from pyexcel_ods import get_data, save_data
from collections import OrderedDict

latmin = 19.039645023697126
latmax = 19.087125696060983
lngmin = 72.88591886230472
lngmax = 72.92025113769535
def process_inputs(t):
    # x=cos(t['lat'])*cos(t['lng'])
    # y=cos(t['lat'])*sin(t['lng'])
    # z=sin(t['lat'])
    
    lat = (t['lat'] - latmin)/(latmax - latmin)
    lng = (t['lng'] - lngmin)/(lngmax - lngmin)
    weekday = t['weekday']/6
    hour    = t['hour']/23
    minutes = t['min']/59
    return [lat, lng, weekday, hour, minutes]

def fil(t):
    return t['hour'] > 15 and t['hour'] < 23

def load_data(dir):
    print('Loading data...', end='', flush=True)
    tuple_paths = glob.glob(dir+'/**/*.json')
    data = []
    for path in tuple_paths:
        with open(path) as json_data:
            d = json.load(json_data)
        data.extend(d)
    # data = list(filter(fil, data))
    shuffle(data)
    training_set = {'x': [], 'y': []}
    test_set = {'x': [], 'y': []}
    y = list(map(lambda t: t.pop('t'), data))
    x = list(map(process_inputs, data))
    total = len(x)
    n_train = (int)(total*0.8)
    for i in range(n_train):
        training_set['x'].append(x[i])
        training_set['y'].append(y[i])
    for i in range(n_train,total):
        test_set['x'].append(x[i])
        test_set['y'].append(y[i])
    print('done')
    return [training_set, test_set]

def merge_csvs(n):
    files = []
    for i in range(n):
        f = input('Enter filename: ')
        files.append(f)
    data = []
    for f in files:
        d = get_data(f)[f]
        data.extend(d)
    shuffle(data)
    shuffle(data)
    shuffle(data)
    col_names = []
    for i in range(len(data[0])):
        c = input('Enter column name: ')
        col_names.append(c)
    mdata = [col_names]
    mdata.extend(data)
    mcsv = OrderedDict([('', mdata)])
    save_data('v4.csv', mcsv)

def save_csv(dir):
    def process_inputs(t):
        # x=cos(t['lat'])*cos(t['lng'])
        # y=cos(t['lat'])*sin(t['lng'])
        # z=sin(t['lat'])
        max = 1
        min = -1
        range = max - min
        lat = (t['lat'] - latmin)/(latmax - latmin) * range + min
        lng = (t['lng'] - lngmin)/(lngmax - lngmin) * range + min
        weekday = t['weekday']/6 * range + min
        hour    = t['hour']/23 * range + min
        minutes = (t['min']//15*15)/45 * range + min
        t = t['t']
        return [lat, lng, weekday, hour, minutes, t]
    print('Loading data...', end='', flush=True)
    tuple_paths = glob.glob(dir+'/**/*.json')
    data = []
    for path in tuple_paths:
        with open(path) as json_data:
            d = json.load(json_data)
        data.extend(d)
    # data = list(filter(fil, data))
    # shuffle(data)
    d = [['latitude', 'longitude', 'weekday', 'hour', 'min', 'traffic']]
    d.extend(list(map(process_inputs, data)))
    csv=OrderedDict([('', d)])
    save_data('v1.csv', csv)
    print('done')

def export_model(model, model_name, num_input):
    print('Exporting model...', end='', flush=True)
    # Export trained model
    def serving_input_receiver_fn():
        inputs = {"x": tf.placeholder(shape=[None, num_input], dtype=tf.float32)}
        return tf.estimator.export.ServingInputReceiver(inputs, inputs)
    export_dir='meta/{}_export'.format(model_name)
    model.export_savedmodel(export_dir_base=export_dir, serving_input_receiver_fn=serving_input_receiver_fn)
    print('done')

def confusion_matrix(labels, predicted):
    # row is predicted as column
    labels = np.array(labels)
    predicted = np.array(predicted)
    l1 = len(labels)
    l2 = len(predicted)
    n_classes = len(np.unique(labels))
    if l1 != l2:
        print('Error in confusion matrix')
    cfmat = np.zeros((n_classes, n_classes),dtype=int)
    for i in range(l1):
        cfmat[labels[i]][predicted[i]] += 1
    print('confusion matrix', cfmat, sep='\n')