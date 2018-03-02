import numpy as np
from pyexcel_ods import get_data, save_data
from collections import OrderedDict
import random

def oversample(data, count):
    icount = len(data)
    n_full_replicate = count // icount
    odata = np.copy(data)
    for i in range(n_full_replicate-1):
        odata = np.concatenate((odata, data))
    lastfew = count - len(odata)
    if lastfew == 0:
        return odata
    try:
        randominds = random.sample(range(icount), lastfew)
        ldata = data[randominds]
        odata = np.concatenate((odata, ldata))
    except ValueError:
        print('Sample size exceeded population size')
    return odata

print('Loading data...', end='', flush=True)
csv = get_data('v1.csv')['v1.csv']
col_names = csv[0]
data = np.array(csv[1:])
t = data[:,5].astype(int)
print('done')
print('Collecting reds...', end='', flush=True)
r = np.where(t == 2)
rdata = np.array(data[r])
print('done')
# print('Collecting dark reds...', end='', flush=True)
# dr = np.where(t == 3)
# drdata = np.array(data[dr])
# nt = np.full(len(drdata), 2)
# drdata[:,5] = nt
# print('done')
final_count = 400000
# print('Oversampling reds...', end='', flush=True)
# rdata=oversample(rdata, final_count)
# print('done')
# print('Oversampling dark reds...', end='', flush=True)
# drdata=oversample(drdata, final_count)
# print('done')
# ocsv = np.concatenate((rdata, drdata))
ocsv = OrderedDict([('', rdata)])
save_data('redndarkred.csv', ocsv)