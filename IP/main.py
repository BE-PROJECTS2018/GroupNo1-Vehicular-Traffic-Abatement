import cv2
import numpy as np
import os, glob, json

class MyEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, np.integer):
            return int(obj)
        elif isinstance(obj, np.floating):
            return float(obj)
        elif isinstance(obj, np.ndarray):
            return obj.tolist()
        else:
            return super(MyEncoder, self).default(obj)

images = glob.glob('../ss gen/images/**/*.png')
for image in images:
    oppath  = image[image.find('\\')+1:]
    opfname = oppath[oppath.find('\\')+1:]      #filename for output image
    opfldr  = oppath[:oppath.find('\\')]        #folder name for output eg. section1
    image   = image.replace('\\','/')           #making it compatible to open input image
    frame   = cv2.imread(image)
    tmp     = opfname.replace('.png','')
    ind     = tmp.find('-')
    week    = tmp[:ind]
    hour    = tmp[ind+1:]
    traffic = []  # {lat, lng, week, hour, t}
    # green
    gind = np.where((frame == (80,202,132)).all(axis=2))
    for i in range(gind[0].size):
        traffic.append({'lat':gind[1][i], 'lng':gind[0][i], 'week': week, 'hour': hour, 't':0})
    # orange    
    oind = np.where((frame == (2,125,240)).all(axis=2))
    for i in range(oind[0].size):
        traffic.append({'lat':oind[1][i], 'lng':oind[0][i], 'week': week, 'hour': hour, 't':1})
    # red
    rind = np.where((frame == (0,0,230)).all(axis=2))
    for i in range(rind[0].size):
        traffic.append({'lat':rind[1][i], 'lng':rind[0][i], 'week': week, 'hour': hour, 't':2})
    # dark red    
    drind = np.where((frame == (19,19,158)).all(axis=2))
    for i in range(drind[0].size):
        traffic.append({'lat':drind[1][i], 'lng':drind[0][i], 'week': week, 'hour': hour, 't':3})
    
    opfldr = os.path.join('json',opfldr)
    opfname = opfname.replace('.png', '.json')
    if not os.path.exists(opfldr):
        os.makedirs(opfldr)
    with open(os.path.join(opfldr, opfname), 'w') as f:
        json.dump(traffic, f, cls=MyEncoder)
cv2.destroyAllWindows()
