import numpy as np
from pyexcel_ods import get_data, save_data
from collections import OrderedDict

csv = get_data('v2.csv')['v2.csv']
col_names = csv[0]
data = np.array(csv[1:])
y = data[:,6:]
nozeros = np.where((y != (0.0,0.0,0.0)).any(axis=1))[0]
newdata = [col_names]
newdata.extend(data[nozeros])
newdata = OrderedDict([('', newdata)])
save_data('v3.csv', newdata)