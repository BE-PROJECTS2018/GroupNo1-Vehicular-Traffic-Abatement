from utils import load_data
import tensorflow as tf
import numpy as np

num_input = 5
LEARNING_RATE = 0.1
NUM_CLASSES = 4
num_steps=100

[training_set, test_set] = load_data('data')
#lat = tf.contrib.layers.real_valued_column(training_set['x'][0])
#lng = tf.contrib.layers.real_valued_column(training_set['x'][1])
#weekday = tf.contrib.layers.real_valued_column(training_set['x'][2])
#hour = tf.contrib.layers.real_valued_column(training_set['x'][3])
#mins = tf.contrib.layers.real_valued_column(training_set['x'][4])
feature_column_data = training_set['x']
feature_tensor = tf.constant(feature_column_data)
feature_columns = [tf.contrib.layers.real_valued_column(feature_tensor,dimension=4)]
#sparse_feature_column = tf.contrib.layers.sparse_column_with_hash_bucket(...)
# real_feature_column = [tf.feature_column.numeric_column("x", shape=[num_input])]

# instantiate the SVM class
#classifier = SVM(alpha=LEARNING_RATE, batch_size=BATCH_SIZE, svm_c=arguments.svm_c, num_classes=NUM_CLASSES,
#    num_features=feature_columns)

# SVM Class - 2
estimator = tf.contrib.learn.SVM(example_id_column = 'example_id',
                feature_columns = feature_columns,
                weight_column_name = None,
                model_dir = None,
                l1_regularization = 0.0,
                l2_regularization = 0.0,
                num_loss_partitions = 1,
                kernels = None,
                config = None,
                feature_engineering_fn = None);


# train the instantiated model
#classifier.train(epochs=arguments.num_epochs, log_path=arguments.log_path, train_data=[training_set['x'], training_set['y']],
#    train_size=training_set.shape[0], validation_data=[test_set['x'], test_set['y']],
#    validation_size=test_set[0], result_path=arguments.result_path)


# Define the training inputs
train_input_fn = tf.estimator.inputs.numpy_input_fn(
    x={"x": np.array(training_set['x'])},
    y=np.array(training_set['y']),
    batch_size = len(training_set['x']),
    num_epochs=num_steps,
    shuffle =True)
print('Training...', end='')

# Train model.
#classifier.train(input_fn=train_input_fn)

#print('done')
# Define the test inputs
test_input_fn = tf.estimator.inputs.numpy_input_fn(
    x={"x": np.array(test_set['x'])},
    y=np.array(test_set['y']),
    batch_size = len(test_set['x']),
    num_epochs = 1,
    shuffle = False)
print('Test...', end='')

input_fn=None,
estimator.fit(input_fn = train_input_fn)
estimator.evaluate(input_fn = test_input_fn)
accuracy_score = estimator.predict(x=x)
print(accuracy_score)

# Evaluate accuracy.
#accuracy_score = classifier.evaluate(input_fn=test_input_fn)
#print(accuracy_score)



