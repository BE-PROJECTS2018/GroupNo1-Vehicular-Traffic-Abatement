from sklearn.naive_bayes import BernoulliNB
# from sklearn.neural_network import MLPClassifier
# from sklearn.svm import NuSVC
# from sklearn.ensemble import RandomForestClassifier, AdaBoostClassifier
# from sklearn.ensemble import ExtraTreesClassifier
from sklearn.metrics import accuracy_score, confusion_matrix
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.utils.class_weight import compute_class_weight
from time import time
import numpy as np
from pyexcel_ods import get_data, save_data
from collections import OrderedDict
from sklearn_porter import Porter
import os

def arrange(arr, ord):
    newarr = np.zeros(4)
    for i in range(len(ord)):
        newarr[ord[i]] = arr[i]
    return newarr

sets = [0,3,6,9,12,15,18,21,23]
acc = np.zeros(len(sets)-1)
predicted = np.zeros(4)
expected = np.zeros(4)
for s in range(1, len(sets)):
    print('Loading data...',end='', flush=True)
    model = 'v1-1-geohash-1-{}{}'.format(sets[s-1], sets[s])
    raw = get_data('{}.csv'.format(model))['{}.csv'.format(model)]
    data = raw[1:]
    geo = np.array(data)[:,0]
    gle = LabelEncoder()
    geo = gle.fit_transform(geo)
    geo_label_map = np.c_[gle.classes_, gle.transform(gle.classes_)]
    geo_label_map = OrderedDict([('', geo_label_map)])
    save_data('{}-map.csv'.format(model), geo_label_map)
    X = []
    y = []
    for i,d in enumerate(data):
        # if d[-1] == 3:
        #     continue
        t = [geo[i]]
        t.extend(d[1:-2])
        t.extend((np.array(d[1:-2])**2).tolist())
        # t.extend((np.array(d[1:-1])**0.5).tolist())
        X.append(t)
        y.append(d[-1])
    print('done')
    
    cl_weights = compute_class_weight(class_weight='balanced', classes=np.unique(y), y=y)
    clw_dict = {}
    for i in range(len(cl_weights)):
        clw_dict[i] = cl_weights[i]
    clf1 = BernoulliNB(alpha=0.5)
    # clf1 = MLPClassifier(hidden_layer_sizes = (10,10,10), activation='relu', solver='sgd', warm_start=True, max_iter = 100, learning_rate='adaptive')
    # clf1 = RandomForestClassifier()
    # clf1 = NuSVC(nu=0.01, kernel='linear', gamma=0.1, tol=0.001, shrinking=False, class_weight='balanced')
    # clf1 = ExtraTreesClassifier(max_depth=20, n_estimators=10, class_weight=clw_dict, min_samples_leaf=1, warm_start=True)
    # clf1 = AdaBoostClassifier(algorithm='SAMME', base_estimator=LinearSVC(tol=0.01, penalty='l1', dual=False, C=2.5, class_weight='balanced', max_iter=100000))
    t1 = time()
    for i in range(50):
        X_train , X_test , y_train , y_test = train_test_split(X,y,test_size=0.2)
        # counts = np.unique(y_train, return_counts=True)
        # counts = arrange(counts[1], counts[0])
        # ratios = np.zeros(counts.shape)
        # for i in range(counts.size):
        #     ratios[i] = counts[i]/sum(counts)
        # print('Ratios of classes', ratios*100)
        # # m = max(counts)
        # # for i in range(counts.size):
        # #     ratios[i] = m/counts[i]
        # ratios = 1 - ratios
        # print('Weights to penalize loss function', ratios)
        trweights = np.zeros(len(X_train))
        for i in range(len(y_train)):
            trweights[i] = cl_weights[y_train[i]]
        teweights = np.zeros(len(X_test))
        for i in range(len(y_test)):
            teweights[i] = cl_weights[y_test[i]]
        clf1.partial_fit(X_train, y_train, sample_weight=trweights, classes=[0,1,2,3])
        pred = clf1.predict(X_test)
        accuracy = clf1.score(X_test, y_test)
    print('Training time: {:.2f}s'.format(time()-t1))
    print('Test accuracy = {:.2f}'.format(accuracy*100))
    acc[s-1] = accuracy*100
    cfmat = confusion_matrix(y_test, pred)
    print(cfmat)
    predicted += cfmat.sum(axis=0)
    expected += cfmat.transpose().sum(axis=0)
    # porter = Porter(clf1)
    # op = porter.export(export_data=True)
    # with open('{}.java'.format(model), 'w') as f:
    #     f.write(op)
    # os.rename('data.json', '{}.json'.format(model))
    # print(op)
predicted = predicted / sum(predicted) * 100
expected = expected / sum(expected) * 100
print('Final accuracy = {:.2f}'.format(sum(acc)/len(acc)))
print('Expected distribution', expected)
print('Predicted distribution', predicted)