from tensorflow.python.tools import freeze_graph

freeze_graph.freeze_graph(
    input_graph='',input_saver='',input_binary=True,input_checkpoint='',
    restore_op_name='',filename_tensor_name='',clear_devices='',initializer_nodes='',
    input_saved_model_dir='./meta/nn_v2_export/1519389605',
    output_graph = './meta/freeze_nn_v2.pb',
    output_node_names='dnn/logits/BiasAdd'
)