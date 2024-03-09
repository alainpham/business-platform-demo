
- [business platform demo](#business-platform-demo)
  - [architecture diagram](#architecture-diagram)
  - [run the demo with docker](#run-the-demo-with-docker)
  - [run demo on kubernetes](#run-demo-on-kubernetes)
    - [Generate a domain name and wildcard certificates with Letsencrypt](#generate-a-domain-name-and-wildcard-certificates-with-letsencrypt)
    - [Create ingress router](#create-ingress-router)
    - [broker](#broker)
    - [availability-service](#availability-service)
    - [business-hub](#business-hub)
    - [notification-service](#notification-service)
    - [email](#email)

# business platform demo

This is a guide to deploy a set of synchronous and asynchronous microservices in spring boot to observed with & Grafana LGTM OSS stack or Grafana Cloud.

## architecture diagram
![alt text](graphics/architecture.png)

## run the demo with docker

Optionally create a dedicated network

```bash
docker network create --driver=bridge --subnet=172.19.0.0/16 --gateway=172.19.0.1 mainnet
```

Launch broker

```bash
docker run --rm -d --net mainnet \
    -e ANONYMOUS_LOGIN=true \
    --name activemq-artemis -p 61616:61616 -p 8161:8161 apache/activemq-artemis:2.32.0
  
docker stop activemq-artemis
```

Launch apps

```bash
export PROJECT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

docker run --rm -d --net mainnet \
    -p 8080:8080 \
    -e OTEL_JAVAAGENT_ENABLED="true" \
    --name business-hub alainpham/business-hub:${PROJECT_VERSION}
    

docker run --rm -d --net mainnet \
    -e OTEL_JAVAAGENT_ENABLED="true" \
    --name availability-service alainpham/availability-service:${PROJECT_VERSION}
    
docker run --rm -d --net mainnet \
    -e OTEL_JAVAAGENT_ENABLED="true" \
    --name notification-service alainpham/notification-service:${PROJECT_VERSION}

docker run --rm -d --net mainnet \
    -e OTEL_JAVAAGENT_ENABLED="true" \
    -e APP_QUEUE="email" \
    -e OTEL_RESOURCE_ATTRIBUTES=service.name=email,service.namespace=email-ns,service.instance.id=email-cnt,service.version=${PROJECT_VERSION} \
    --name email alainpham/message-consumer:${PROJECT_VERSION}

docker run --rm -d --net mainnet \
    -e OTEL_JAVAAGENT_ENABLED="true" \
    -e APP_QUEUE="sms" \
    -e OTEL_RESOURCE_ATTRIBUTES=service.name=sms,service.namespace=sms-ns,service.instance.id=sms-cnt,service.version=${PROJECT_VERSION} \
    --name sms alainpham/message-consumer:${PROJECT_VERSION}
```

## run demo on kubernetes

The kube deployment yaml files use ingress, to expose application http(s) services to ouside of kubernetes through a loadbalancer type service.

If you want to have clean certificates with valid letsencrypt and a proper domain name follow these optional steps. I use the free duckdns.org dns service.

For this demo to work you this section is optional

this is the architecture we will be building


### Generate a domain name and wildcard certificates with Letsencrypt

Go to duckdns.org and create a domain name as in yourowndomain.duckdns.org. You don't need to specify an IP address yet. We will point it to the 
All applications deployed on kube will be accessible with an url like appname.yourowndomain.duckdns.org

```bash
# https://github.com/infinityofspace/certbot_dns_duckdns
export CERTBOT_DUCKDNS_VERSION=v1.3
export DUCKDNS_TOKEN=xxxx-xxxx-xxx-xxx-xxxxx
export EMAIL=xxx@yyy.com
export WILDCARD_DOMAIN=yourowndomain.duckdns.org

docker run -v "$(pwd)/sensitive/letsencrypt/data:/etc/letsencrypt" -v "$(pwd)/sensitive/letsencrypt/logs:/var/log/letsencrypt" infinityofspace/certbot_dns_duckdns:${CERTBOT_DUCKDNS_VERSION} \
   certonly \
     --non-interactive \
     --agree-tos \
     --email ${EMAIL} \
     --preferred-challenges dns \
     --authenticator dns-duckdns \
     --dns-duckdns-token ${DUCKDNS_TOKEN} \
     --dns-duckdns-propagation-seconds 15 \
     -d "*.${WILDCARD_DOMAIN}"

sudo chown -R ${USER}:${USER} $(pwd)/sensitive/letsencrypt/data
```

### Create ingress router

```bash
kubectl create ns ingress-nginx

kubectl -n ingress-nginx create  secret tls nginx-ingress-tls  --key="$(pwd)/sensitive/letsencrypt/data/live/$WILDCARD_DOMAIN/privkey.pem"   --cert="$(pwd)/sensitive/letsencrypt/data/live/$WILDCARD_DOMAIN/fullchain.pem"  --dry-run=client -o yaml | kubectl apply -f -


# https://github.com/kubernetes/ingress-nginx/blob/main/deploy/static/provider/baremetal/deploy.yaml
export NGINX_INGRESS_VERSION=1.10.0
export NGINX_INGRESS_KUBE_WEBHOOK_CERTGEN_VERSION=v1.4.0

#ingress with LoadBalancer
wget -O /tmp/ingresslb.yaml https://raw.githubusercontent.com/alainpham/dev-environment/master/workstation-installation/templates/ingress-loadbalancer-notoleration.yaml
envsubst < /tmp/ingresslb.yaml | kubectl -n ingress-nginx apply -f -


# alternative ingress with hostport on non cloud instances
wget -O /tmp/ingress.yaml https://raw.githubusercontent.com/alainpham/dev-environment/master/workstation-installation/templates/ingress-hostport-notoleration.yaml
envsubst < /tmp/ingress.yaml | kubectl -n ingress-nginx apply -f -

```

### broker

```bash
export KUBE_INGRESS_ROOT_DOMAIN=yourowndomain.duckdns.org
export CONTAINER_REGISTRY=apache
export PROJECT_ARTIFACTID=activemq-artemis
export PROJECT_VERSION=2.32.0

kubectl create ns business-platform

wget -O /tmp/broker.yaml https://raw.githubusercontent.com/alainpham/business-platform-demo/master/broker/broker.envsubst.yaml
envsubst < /tmp/broker.yaml | kubectl apply -n business-platform -f -

envsubst < /tmp/broker.yaml | kubectl delete -n business-platform -f -
```

### availability-service

```bash
export KUBE_INGRESS_ROOT_DOMAIN=yourowndomain.duckdns.org
export PROJECT_ARTIFACTID=availability-service
export PROJECT_VERSION=1.0-SNAPSHOT
export CONTAINER_REGISTRY=alainpham

wget -O /tmp/availability-service.yaml https://raw.githubusercontent.com/alainpham/business-platform-demo/master/availability-service/src/main/kube/deploy.envsubst.yaml
envsubst < /tmp/availability-service.yaml | kubectl apply -n business-platform -f -

envsubst < /tmp/availability-service.yaml | kubectl delete -n business-platform  -f -
```

### business-hub

```bash
export KUBE_INGRESS_ROOT_DOMAIN=yourowndomain.duckdns.org
export PROJECT_ARTIFACTID=business-hub
export PROJECT_VERSION=1.0-SNAPSHOT
export CONTAINER_REGISTRY=alainpham

wget -O /tmp/business-hub.yaml https://raw.githubusercontent.com/alainpham/business-platform-demo/master/business-hub/src/main/kube/deploy.envsubst.yaml
envsubst < /tmp/business-hub.yaml | kubectl apply -n business-platform  -f -

envsubst < /tmp/business-hub.yaml | kubectl delete -n business-platform  -f -
```

### notification-service

```bash
export KUBE_INGRESS_ROOT_DOMAIN=yourowndomain.duckdns.org
export PROJECT_ARTIFACTID=notification-service
export PROJECT_VERSION=1.0-SNAPSHOT
export CONTAINER_REGISTRY=alainpham

wget -O /tmp/notification-service.yaml https://raw.githubusercontent.com/alainpham/business-platform-demo/master/notification-service/src/main/kube/deploy.envsubst.yaml
envsubst < /tmp/notification-service.yaml | kubectl apply -n business-platform  -f -

envsubst < /tmp/notification-service.yaml | kubectl delete -n business-platform  -f -
```

### email

```bash
export KUBE_INGRESS_ROOT_DOMAIN=yourowndomain.duckdns.org
export PROJECT_ARTIFACTID=message-consumer
export APPLICATION_NAME=email
export PROJECT_VERSION=1.0-SNAPSHOT
export CONTAINER_REGISTRY=alainpham

wget -O /tmp/message-consumer.yaml https://raw.githubusercontent.com/alainpham/business-platform-demo/master/message-consumer/src/main/kube/deploy.envsubst.yaml
envsubst < /tmp/message-consumer.yaml | kubectl apply -n business-platform  -f -

envsubst < /tmp/message-consumer.yaml | kubectl delete -n business-platform  -f -
```
