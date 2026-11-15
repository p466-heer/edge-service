
allow_k8s_contexts('polar')

k8s_yaml([
    'k8s/deployment.yml',
    'k8s/service.yml'
])

k8s_resource(
    'edge-service',
    port_forwards=['9000:80']
)
