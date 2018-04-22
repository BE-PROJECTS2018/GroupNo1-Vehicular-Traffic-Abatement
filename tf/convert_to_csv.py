import json, glob
from random import shuffle
from pyexcel_ods import get_data, save_data
from collections import OrderedDict
import geohash as gh
import numpy as np
from time import time

def fil03(t):
    return t['hour'] >= 0 and t['hour'] <= 3

def fil36(t):
    return t['hour'] > 3 and t['hour'] <= 6

def fil69(t):
    return t['hour'] > 6 and t['hour'] <= 9

def fil912(t):
    return t['hour'] > 9 and t['hour'] <= 12

def fil1215(t):
    return t['hour'] > 12 and t['hour'] <= 15

def fil1518(t):
    return t['hour'] > 15 and t['hour'] <= 18

def fil1821(t):
    return t['hour'] > 18 and t['hour'] <= 21

def fil2123(t):
    return t['hour'] > 21 and t['hour'] <= 23

def process_inputs(t):
    # x=cos(t['lat'])*cos(t['lng'])
    # y=cos(t['lat'])*sin(t['lng'])
    # z=sin(t['lat'])
    max = 1
    min = -1
    range = max - min
    lat = round(t['lat'], 7)# - latmin)/(latmax - latmin) * range + min
    lng = round(t['lng'], 7)# - lngmin)/(lngmax - lngmin) * range + min
    weekday = t['weekday']#/6 * range + min
    hour    = t['hour']#/23 * range + min
    minutes = (t['min']//15*15)#/45 * range + min
    t = t['t']
    return [lat, lng, weekday, hour, minutes, t]

def process_inputs_as_hash(t):
    # x=cos(t['lat'])*cos(t['lng'])
    # y=cos(t['lat'])*sin(t['lng'])
    # z=sin(t['lat'])
    max = 1
    min = -1
    range = max - min
    lat = round(t['lat'], 7)# - latmin)/(latmax - latmin) * range + min
    lng = round(t['lng'], 7)# - lngmin)/(lngmax - lngmin) * range + min
    hash = gh.encode(lat, lng, 7)
    weekday = t['weekday']#/6 * range + min
    hour    = t['hour']#/23 * range + min
    minutes = (t['min']//15*15)#/45 * range + min
    seconds = int(weekday)*24*60*60 + int(hour)*60*60 + int(minutes)*60
    t = t['t']
    return [hash, weekday, hour, minutes, seconds, t]

def save_csv(dir):
    print('Loading data...', end='', flush=True)
    tuple_paths = glob.glob(dir+'/**/*.json')
    data = []
    for path in tuple_paths:
        with open(path) as json_data:
            d = json.load(json_data)
        data.extend(d)
    data03 = list(filter(fil03, data))
    data36 = list(filter(fil36, data))
    data69 = list(filter(fil69, data))
    data912 = list(filter(fil912, data))
    data1215 = list(filter(fil1215, data))
    data1518 = list(filter(fil1518, data))
    data1821 = list(filter(fil1821, data))
    data2123 = list(filter(fil2123, data))
    # shuffle(data)
    d = []
    d = [['latitude', 'longitude', 'weekday', 'hour', 'min', 'traffic']]
    d.extend(list(map(process_inputs, data03)))
    csv=OrderedDict([('', d)])
    save_data('v1-1-03.csv', csv)
    d = []
    d = [['latitude', 'longitude', 'weekday', 'hour', 'min', 'traffic']]
    d.extend(list(map(process_inputs, data36)))
    csv=OrderedDict([('', d)])
    save_data('v1-1-36.csv', csv)
    d = []
    d = [['latitude', 'longitude', 'weekday', 'hour', 'min', 'traffic']]
    d.extend(list(map(process_inputs, data69)))
    csv=OrderedDict([('', d)])
    save_data('v1-1-69.csv', csv)
    d = []
    d = [['latitude', 'longitude', 'weekday', 'hour', 'min', 'traffic']]
    d.extend(list(map(process_inputs, data912)))
    csv=OrderedDict([('', d)])
    save_data('v1-1-912.csv', csv)
    d = []
    d = [['latitude', 'longitude', 'weekday', 'hour', 'min', 'traffic']]
    d.extend(list(map(process_inputs, data1215)))
    csv=OrderedDict([('', d)])
    save_data('v1-1-1215.csv', csv)
    d = []
    d = [['latitude', 'longitude', 'weekday', 'hour', 'min', 'traffic']]
    d.extend(list(map(process_inputs, data1518)))
    csv=OrderedDict([('', d)])
    save_data('v1-1-1518.csv', csv)
    d = []
    d = [['latitude', 'longitude', 'weekday', 'hour', 'min', 'traffic']]
    d.extend(list(map(process_inputs, data1821)))
    csv=OrderedDict([('', d)])
    save_data('v1-1-1821.csv', csv)
    d = []
    d = [['latitude', 'longitude', 'weekday', 'hour', 'min', 'traffic']]
    d.extend(list(map(process_inputs, data2123)))
    csv=OrderedDict([('', d)])
    save_data('v1-1-2123.csv', csv)
    print('done')

def del_dups(data):
    ndata = []
    data = np.array(data)
    x = data[:,:-1]
    y = data[:,-1].astype(int)
    print('Finding unique records...', end='', flush=True)
    ux = np.unique(x, axis=0)
    print('done')
    print('Summarizing predictions...', end='', flush=True)
    for i in range(len(ux)):
        xi = np.argwhere((x == ux[i]).all(axis=1)).flatten()
        yi = y[xi]
        uy = int(np.argmax(np.bincount(yi)))
        tup = []
        for j in range(len(ux[i])):
            if j == 0:
                tup.append(ux[i][j])
            else:
                tup.append(ux[i][j].astype(int))
        tup.append(uy)
        ndata.append(tup)
    print('done')
    return ndata

def save_csvas_hash(dir):
    t1 = time()
    print('Loading data...', end='', flush=True)
    tuple_paths = glob.glob(dir+'/**/*.json')
    data = []
    for path in tuple_paths:
        with open(path) as json_data:
            d = json.load(json_data)
        data.extend(d)
    print('done')
    # d1 = list(filter(fil1821, data))
    # d=[]
    cols = ['geohash', 'weekday', 'hour', 'min', 'seconds', 'traffic']
    # d.extend(list(map(process_inputs_as_hash, d1)))
    # csv=OrderedDict([('', d)])
    # save_data('v1-1-geohash.csv', csv)
    ranges = [3,6,9,12,15,18,21,23]
    fname = 'v1-1-geohash-1-0{}.csv'.format(ranges[0])
    ndata = list(filter(lambda t: t['hour'] > -1 and t['hour'] <=ranges[0], data))
    ndata = list(map(process_inputs_as_hash, ndata))
    ndata = del_dups(ndata)
    d = [cols[:]]
    d.extend(ndata)
    csv = OrderedDict([('', d)])
    save_data(fname, csv)
    for i in range(1, len(ranges)):
        fname = 'v1-1-geohash-1-{}{}.csv'.format(ranges[i-1], ranges[i])
        ndata = list(filter(lambda t: t['hour'] > ranges[i-1] and t['hour'] <=ranges[i], data))
        ndata = list(map(process_inputs_as_hash, ndata))
        ndata = del_dups(ndata)
        d = [cols[:]]
        d.extend(ndata)
        csv = OrderedDict([('', d)])
        save_data(fname, csv)
    print('Time taken {:.2f}s'.format(time()-t1))
# save_csv('./../IP/json')
save_csvas_hash('./../IP/json')