



export CONTAINER_REGISTRY=apache
export PROJECT_ARTIFACTID=activemq-artemis
export PROJECT_VERSION=2.32.0
export KUBE_INGRESS_ROOT_DOMAIN=gkube.duckdns.org

kubectl create namespace broker

envsubst < broker.envsubst.yaml | kubectl delete -n broker -f -
envsubst < broker.envsubst.yaml | kubectl apply -n broker -f -