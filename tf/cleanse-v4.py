from pyexcel_ods import get_data, save_data
import numpy as np
from collections import OrderedDict

raw = get_data('v4.csv')['v4.csv']
cols = raw[0]
# data = np.array(raw[1:])
# X = data[:,:-1]
# y = data[:,-1].astype(int)
mini = -1
max = 1
ran = max - mini
allowed_weeks = [i/6*ran + mini for i in range(7)]
allowed_hours = [i/23*ran + mini for i in range(24)]
allowed_mins = [i/45*ran + mini for i in range(0, 59, 15)]
for i in range(1, len(raw) - 1):
    raw[i][2] = min(allowed_weeks, key=lambda x:abs(x-raw[i][2]))
    raw[i][3] = min(allowed_hours, key=lambda x:abs(x-raw[i][3]))
    raw[i][4] = min(allowed_mins, key=lambda x:abs(x-raw[i][4]))
    raw[i][-1] = int(raw[i][-1])

data = raw[1:]
ranges = [3,6,9,12,15,18,21,23]
nranges = [v/23*ran + mini for v in ranges]


fname = 'v4-0{}.csv'.format(ranges[0])
ndata = list(filter(lambda t: t[3] >= -1 and t[3] <=nranges[0], data))
d = [cols[:]]
d.extend(ndata)
csv = OrderedDict([('', d)])
save_data(fname, csv)
for i in range(1, len(ranges)):
    fname = 'v4-{}{}.csv'.format(ranges[i-1], ranges[i])
    ndata = list(filter(lambda t: t[3] > nranges[i-1] and t[3] <=nranges[i], data))
    d = [cols[:]]
    d.extend(ndata)
    csv = OrderedDict([('', d)])
    save_data(fname, csv)