import tensorflow as tf
import numpy as np
from pyexcel_ods import get_data, save_data
from collections import OrderedDict

import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'

print('Loading data...', end='', flush=True)
csv = get_data('v1.csv')['v1.csv']
col_names = csv[0]
data = np.array(csv[1:])
t = data[:,5].astype(int)
print('done')
print('Collecting greens...', end='', flush=True)
g = np.where(t == 0)
gdata = np.array(data[g])
print('done')
print('Collecting oranges...', end='', flush=True)
o = np.where(t == 1)
odata = np.array(data[o])
print('done')
final_count = 400000
greenKmeans = tf.contrib.factorization.KMeansClustering(
    num_clusters = final_count,
    model_dir = './meta/green_kmeans',
    use_mini_batch = False
)

orangeKmeans = tf.contrib.factorization.KMeansClustering(
    num_clusters = final_count,
    model_dir = './meta/orange_kmeans',
    use_mini_batch = False
)

input_fn=lambda: tf.train.limit_epochs(tf.convert_to_tensor(gdata[:,:5], dtype=tf.float32), num_epochs=1)

print('Training green...', flush=True)
# Train model.
previous_centers = None
for _ in range(10):
    greenKmeans.train(input_fn)
    centers = np.array(greenKmeans.cluster_centers())
    if previous_centers is not None:
        delta = centers - previous_centers
        print(sum(delta)/len(delta), flush=True)
    previous_centers = centers
    print ('score:', greenKmeans.score(input_fn), flush=True)
print('done', flush=True)
greendata = np.zeros((final_count, 1), dtype=int)
greendata = np.concatenate((centers, greendata), axis=1)
gcsv = OrderedDict([('', greendata)])
save_data('green.csv', gcsv)

input_fn=lambda: tf.train.limit_epochs(tf.convert_to_tensor(odata[:,:5], dtype=tf.float32), num_epochs=1)

print('Training orange...', flush=True)
# Train model.
previous_centers = None
for _ in range(10):
    orangeKmeans.train(input_fn)
    centers = np.array(orangeKmeans.cluster_centers())
    if previous_centers is not None:
        delta = centers - previous_centers
        print(sum(delta)/len(delta), flush=True)
    previous_centers = centers
    print ('score:', orangeKmeans.score(input_fn), flush=True)
print('done', flush=True)
orangedata = np.ones((final_count, 1), dtype=int)
orangedata = np.concatenate((centers, orangedata), axis=1)
ocsv = OrderedDict([('', orangedata)])
save_data('orange.csv', ocsv)