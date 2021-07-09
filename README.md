# AirpurifierRentalPJT
21년  개인과제
# PurifierRentalProject (공기청정기렌탈 서비스 - 개인)

공기청정기 렌탈 신청 서비스 프로젝트 - 개인 과제 입니다.

# Table of contents

- [PurifierRentalProject (공기청정기렌탈 신청 서비스)](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현:](#구현-)
    - [DDD 의 적용](#ddd-의-적용)
    - [Gateway 적용](#Gateway-적용)
    - [CQRS](#CQRS)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [동기식 호출과 Fallback 처리](#동기식-호출과-Fallback-처리)
  - [운영](#운영)
    - [Deploy/Pipeline](#Deploy-Pipeline)
    - [Autoscale(HPA)](#Autoscale)
    - [Zero-Downtime deploy(Readiness Probe)](#Zero-Downtime-deploy(Readiness-Probe))
    - [Self-healing(Liveness Probe)](#Self-healing)

# 서비스 시나리오

고객이 공기청정기 렌탈 서비스 가입신청을 하면 설치기사가 방문하여 설치를 하고, 가입 취소 시 취소 처리를 할 수 있도록 한다.

기능적 요구사항
1. 고객이 공기청정기 렌탈 서비스 가입신청을 한다.
1. 가입신청 접수가 되면, 자동으로 시스템이 가입요청 지역의 설치 기사에게 설치 요청이 된다.
1. 설치기사는 설치요청을 할당받는다.
1. 설치기사는 설치를 완료 후 설치 완료 처리를 한다.
1. 설치가 완료되면 공기청정기 렌탈 서비스 신청이 완료 처리가 된다.
1. 고객이 가입 신청을 취소할 수 있다.
1. 가입신청이 취소되면 설치 취소된다.(설치취소 처리는 Req/Res 테스트를 위해 임의로 동기처리)
1. 고객은 설치진행상태를 수시로 확인할 수 있다.

(개인과제) 기능적 요구사항 추가
1. 가입신청이 완료되면 고객정보를 저장한다.(통계정보 활용, 대시보드)
2. 고객 정보 저장 후 요청이 완료됨(요청종료 처리는 Req/Res 테스트를 위해 임의로 동기처리)
3. 고객정보 상태값과 정보를 관련자가 수시로 확인할 수 있다.

비기능적 요구사항
1. 트랜잭션
    1. 가입취소 신청은 설치취소가 동시 이루어 지도록 한다
    2. (개인과제)가입신청 종료는 정보수집과 동시에 이루어지도록 한다.
    
1. 장애격리
    1. 공기청정기 렌탈 가입신청과 취소는 고객서비스 담당자의 접수, 설치 처리와 관계없이 항상 처리 가능하다. 
    2. 가입처리는 고객정보 수집과 관계없이 항상 처리 가능하다.

1. 성능
    1. 고객은 주문/설치 진행상태를 수시로 확인한다.(CQRS)
    2. (개인과제)관련자는 고객정보를 수시로 확인한다.(CQRS)



# 체크포인트

- 분석 설계


  - 이벤트스토밍: 
    - 스티커 색상별 객체의 의미를 제대로 이해하여 헥사고날 아키텍처와의 연계 설계에 적절히 반영하고 있는가?
    - 각 도메인 이벤트가 의미있는 수준으로 정의되었는가?
    - 어그리게잇: Command와 Event 들을 ACID 트랜잭션 단위의 Aggregate 로 제대로 묶었는가?
    - 기능적 요구사항과 비기능적 요구사항을 누락 없이 반영하였는가?    

  - 서브 도메인, 바운디드 컨텍스트 분리
    - 팀별 KPI 와 관심사, 상이한 배포주기 등에 따른  Sub-domain 이나 Bounded Context 를 적절히 분리하였고 그 분리 기준의 합리성이 충분히 설명되는가?
      - 적어도 3개 이상 서비스 분리
    - 폴리글랏 설계: 각 마이크로 서비스들의 구현 목표와 기능 특성에 따른 각자의 기술 Stack 과 저장소 구조를 다양하게 채택하여 설계하였는가?
    - 서비스 시나리오 중 ACID 트랜잭션이 크리티컬한 Use 케이스에 대하여 무리하게 서비스가 과다하게 조밀히 분리되지 않았는가?
  - 컨텍스트 매핑 / 이벤트 드리븐 아키텍처 
    - 업무 중요성과  도메인간 서열을 구분할 수 있는가? (Core, Supporting, General Domain)
    - Request-Response 방식과 이벤트 드리븐 방식을 구분하여 설계할 수 있는가?
    - 장애격리: 서포팅 서비스를 제거 하여도 기존 서비스에 영향이 없도록 설계하였는가?
    - 신규 서비스를 추가 하였을때 기존 서비스의 데이터베이스에 영향이 없도록 설계(열려있는 아키택처)할 수 있는가?
    - 이벤트와 폴리시를 연결하기 위한 Correlation-key 연결을 제대로 설계하였는가?

  - 헥사고날 아키텍처
    - 설계 결과에 따른 헥사고날 아키텍처 다이어그램을 제대로 그렸는가?
    
- 구현
  - [DDD] 분석단계에서의 스티커별 색상과 헥사고날 아키텍처에 따라 구현체가 매핑되게 개발되었는가?
    - Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 데이터 접근 어댑터를 개발하였는가
    - [헥사고날 아키텍처] REST Inbound adaptor 이외에 gRPC 등의 Inbound Adaptor 를 추가함에 있어서 도메인 모델의 손상을 주지 않고 새로운 프로토콜에 기존 구현체를 적응시킬 수 있는가?
    - 분석단계에서의 유비쿼터스 랭귀지 (업무현장에서 쓰는 용어) 를 사용하여 소스코드가 서술되었는가?
  - Request-Response 방식의 서비스 중심 아키텍처 구현
    - 마이크로 서비스간 Request-Response 호출에 있어 대상 서비스를 어떠한 방식으로 찾아서 호출 하였는가? (Service Discovery, REST, FeignClient)
    - 서킷브레이커를 통하여  장애를 격리시킬 수 있는가?
  - 이벤트 드리븐 아키텍처의 구현
    - 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?
    - Correlation-key:  각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 Correlation-key 연결을 제대로 구현 하였는가?
    - Message Consumer 마이크로서비스가 장애상황에서 수신받지 못했던 기존 이벤트들을 다시 수신받아 처리하는가?
    - Scaling-out: Message Consumer 마이크로서비스의 Replica 를 추가했을때 중복없이 이벤트를 수신할 수 있는가
    - CQRS: Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능한가?

  - 폴리글랏 로그래밍
    - 각 마이크로 서비스들이 하나이상의 각자의 기술 Stack 으로 구성되었는가?
    - 각 마이크로 서비스들이 각자의 저장소 구조를 자율적으로 채택하고 각자의 저장소 유형 (RDB, NoSQL, File System 등)을 선택하여 구현하였는가?
  - API 게이트웨이
    - API GW를 통하여 마이크로 서비스들의 집입점을 통일할 수 있는가?
    - 게이트웨이와 인증서버(OAuth), JWT 토큰 인증을 통하여 마이크로서비스들을 보호할 수 있는가?
- 운영
  - SLA 준수
    - 셀프힐링: Liveness Probe 를 통하여 어떠한 서비스의 health 상태가 지속적으로 저하됨에 따라 어떠한 임계치에서 pod 가 재생되는 것을 증명할 수 있는가?
    - 서킷브레이커, 레이트리밋 등을 통한 장애격리와 성능효율을 높힐 수 있는가?
    - 오토스케일러 (HPA) 를 설정하여 확장적 운영이 가능한가?
    - 모니터링, 앨럿팅: 
  - 무정지 운영 CI/CD (10)
    - Readiness Probe 의 설정과 Rolling update을 통하여 신규 버전이 완전히 서비스를 받을 수 있는 상태일때 신규버전의 서비스로 전환됨을 siege 등으로 증명 
    - Contract Test :  자동화된 경계 테스트를 통하여 구현 오류나 API 계약위반를 미리 차단 가능한가?
    - 


# 분석/설계


## 팀Project Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  
  - http://www.msaez.io/#/storming/Cxdgj5SBtvclVuCxWENSvPhVTuD2/mine/04d95b6ac2871ef725a46b9cb7114f58

![2ndDesign](https://user-images.githubusercontent.com/81946287/118765229-bc240280-b8b5-11eb-8bf4-2015470e7987.png)


    
### (개인과제) Management 서비스 추가된 모형
![image](https://user-images.githubusercontent.com/84304047/124922840-dc11b000-e034-11eb-85a4-b116e2d6742a.png)


### 서비스 추가된 완성본에 대한 기능적/비기능적 요구사항을 커버하는지 검증
#### 시나리오 Coverage Check
![image](https://user-images.githubusercontent.com/84304047/124923080-1418f300-e035-11eb-8006-405979ca53e2.png)


# 구현:
분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 마이크로 서비스들을 스프링부트로 구현하였다. 
구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 8084 이다)

```
- Local
	cd Order
	mvn spring-boot:run

	cd Assignment
	mvn spring-boot:run

	cd Installation
	mvn spring-boot:run
	
	cd Management
	mvn spring-boot:run


- EKS : CI/CD 통해 빌드/배포 ("운영 > CI-CD 설정" 부분 참조)
```

## DDD 의 적용
- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다: Order, Assignment, Installation, Management
- Management(고객관리) 마이크로서비스 예시

```
package purifierrentalpjt;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Management_table")
public class Management {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long orderId;
    private Long surveyId;
    private Long customerId;
    private String status;
    private String surveyResult;

    @PostPersist
    public void onPostPersist(){

        System.out.println("=================>>>>" + this.getStatus() + "POST TEST");

        SurveyCompleted surveyCompleted = new SurveyCompleted();

        surveyCompleted.setId(this.getId());
        surveyCompleted.setOrderId(this.orderId);
        surveyCompleted.setStatus(this.getStatus()); 
        BeanUtils.copyProperties(this, surveyCompleted);
        surveyCompleted.publishAfterCommit();


    }


    public Long getId() {
        return id;
    }

```

적용 후 REST API의 테스트
공기청정기 렌탈 서비스 가입완료 후 고객정보 처리

- (a) http -f POST  http://localhost:8081/order/joinOrder productId=1 productName=PURI1 installationAddress="Addr1" customerId=101
- (b) http -f PATCH http://localhost:8083/installations orderId=1 
- (c) http -f PATCH http://localhost:8081/order/saveInfo orderId=1 surveyResult="GOOD"
![image](https://user-images.githubusercontent.com/84304047/125000575-a8ae4000-e08b-11eb-8d63-92add67315a1.png)



## Gateway 적용
API Gateway를 통하여, 마이크로 서비스들의 진입점을 통일한다.

```
server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: order
          uri: http://localhost:8081
          predicates:
            - Path=/order/**,/orders/**,/orderStatuses/**
        - id: assignment
          uri: http://localhost:8082
          predicates:
            - Path=/assignments/**,/assignment/**  
        - id: installation
          uri: http://localhost:8083
          predicates:
            - Path=/installations/**,/installation/**
        - id: management
          uri: http://localhost:8084
          predicates:
            - Path=/managements/**,/management/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true


---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: order
          uri: http://order:8080
          predicates:
            - Path=/order/**,/orders/**,/orderStatuses/**
        - id: assignment
          uri: http://assignment:8080
          predicates:
            - Path=/assignment/**,/assignments/** 
        - id: installation
          uri: http://installation:8080
          predicates:
            - Path=/installation/**,/installations/**
        - id: management
          uri: http://management:8080
          predicates:
            - Path=/management/**,/managements/** 
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080
```

- EKS에 배포 시, MSA는 Service type을 ClusterIP(default)로 설정하여, 클러스터 내부에서만 호출 가능하도록 한다.
- API Gateway는 Service type을 LoadBalancer로 설정하여 외부 호출에 대한 라우팅을 처리한다.


## CQRS
가입신청+정보저장 상태 조회를 위한 서비스를 CQRS 패턴으로 구현하였다.
- Order, Assignment, Installation, Management 개별 aggregate 통합 조회로 인한 성능 저하를 막을 수 있다.
- 모든 정보는 비동기 방식으로 발행된 이벤트를 수신하여 처리된다.
- 설계 : MSAEz 설계의 view 매핑 설정 참조

- 정보저장
![image](https://user-images.githubusercontent.com/84304047/125000391-376e8d00-e08b-11eb-872a-5ebdf85b6fbc.png)
- 카프카 메시지
![image](https://user-images.githubusercontent.com/84304047/125001161-f5dee180-e08c-11eb-9bae-5e26d4f7b316.png)


- 뷰테이블 수신처리(카프카 메세지를 받아서 처리)

![ViewHandler](https://user-images.githubusercontent.com/81946287/120587912-32596500-c471-11eb-8f64-1ee819ddfc25.png)

![ViewHandler2](https://user-images.githubusercontent.com/81946287/120587921-35545580-c471-11eb-86c2-741a5ae8455e.png)

  
## 폴리글랏 퍼시스턴스
Management서비스의 DB 를 HSQL 로 설정하여 MSA간 서로 다른 종류의 DB간에도 문제 없이 동작하여 다형성을 만족하는지 확인하였다.

|서비스|DB|pom.xml|
| :--: | :--: | :--: |
|Management| HSQL |![image](https://user-images.githubusercontent.com/84304047/124858788-b1e6d080-dfe9-11eb-9547-4c98cad2b794.png)|
|Assignment| H2 |![image](https://user-images.githubusercontent.com/84304047/124858681-7cda7e00-dfe9-11eb-991c-2cfde83cf5da.png)|
|Order| H2 |![image](https://user-images.githubusercontent.com/84304047/124858717-89f76d00-dfe9-11eb-815d-b1de020a0581.png)|


## 동기식 호출과 Fallback 처리
- 분석 단계에서의 조건 중 하나로 주문(Order) 서비스에서 정보처리시, 
관리(management) 서비스 정보 처리하는 부분을 동기식 호출하는 트랜잭션으로 처리하기로 하였다. 
- 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어 있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.

saveInfo 를 불러 infoComplted에서 받아 Order 로 넘어가는 처리(req/res)
```
# (Order) ManagementService.java

	package purifierrentalpjt.external;

	import org.springframework.cloud.openfeign.FeignClient;
	import org.springframework.web.bind.annotation.RequestBody;
	import org.springframework.web.bind.annotation.RequestMapping;
	import org.springframework.web.bind.annotation.RequestMethod;

	import java.util.Date;

	@FeignClient(name="Management", url="http://localhost:8084")
	//@FeignClient(name="Management", url="http://management:8080")
	public interface ManagementService {

    		@RequestMapping(method= RequestMethod.POST, path="/managements")
   		 public void saveInfo(@RequestBody Management management);

	}
```

정보 저장이 되면(@PostUpdate) 정보 처리가 되도록 처리
```
# (Order) Order.java

    @PostUpdate
    public void onPostUpdate(){
        /* 정보처리 */
    	System.out.println("### 정보저장 Update and Update Event raised..." + this.getStatus());
        if(this.getStatus().equals("surveySubmit")) {
            InfoCompleted InfoCompleted = new InfoCompleted();
            BeanUtils.copyProperties(this, surveySubmitted);
            surveySubmitted.publishAfterCommit();

            purifierrentalpjt.external.Management management = new purifierrentalpjt.external.Management();

            management.setId(this.getId());

            OrderApplication.applicationContext.getBean(purifierrentalpjt.external.ManagementService.class)
            .completeSurvey(management);
        }


    }
   
```

동기식 호출에서는 호출 시간에 따른 타입 커플링이 발생하며, 관리(Management) 서비스가 장애가 나면 정보처리가 되지 않는다는 것을 확인
![동기호출](https://user-images.githubusercontent.com/81946287/120578039-2022fb00-c460-11eb-8156-dc6aaed13bf4.png)


# 운영

## Deploy/Pipeline
각 구현체들은 각자의 source repository 에 구성되었고, 각 서비스별로 빌드를 하여, aws ecr에 등록 후 deployment.yaml 통해 EKS에 배포함.


- git에서 소스 가져오기

```
git clone https://github.com/bus2431/PurifierRentalPJT
```

- Build 하기

```bash
cd Order
mvn package -B;

cd ..
cd Installation
mvn package -B;

cd ..
cd concert
mvn package -B;

cd ..
cd Assignment
mvn package -B;

cd ..
cd Management
mvn package -B;
```

- aws 이미지 캡처
![image](https://user-images.githubusercontent.com/84304047/124861576-d1ccc300-dfee-11eb-9c64-770618d9542b.png)
![image](https://user-images.githubusercontent.com/84304047/124861602-db562b00-dfee-11eb-88ec-038c9db3e9d2.png)

- PurifierRentalPJT/Management/kubernetes/deployment.yml 파일 

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: management
  labels:
    app: management
spec:
  replicas: 1
  selector:
    matchLabels:
      app: management
  template:
    metadata:
      labels:
        app: management
    spec:
      containers:
        - name: order
          image: XXXXXX.dkr.ecr.eu-west-3.amazonaws.com/user19-management:v1
          ports:
            - containerPort: 8080
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
          env:
          - name: INIT_NAME
            valueFrom:
              secretKeyRef:
                name: order
                key: username
          - name: INIT_PW
            valueFrom:
              secretKeyRef:
                name: order
                key: password
```	  

***

## Autoscale

- metric 서버를 설치한다.

```sh
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.3.7/components.yaml
kubectl get deployment metrics-server -n kube-system
```
- 주문 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 1프로를 넘어서면 replica 를 10개까지 늘려준다.

```
kubectl autoscale deploy order --min=1 --max=10 --cpu-percent=1
```

![autoscale](https://user-images.githubusercontent.com/81946287/120629728-14a3f400-c4a1-11eb-8347-0ed947dc5f0b.png)


- (order)deployment.yml

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order
  labels:
    app: order
spec:
  replicas: 1
  selector:
    matchLabels:
      app: order
  template:
    metadata:
      labels:
        app: order
    spec:
      containers:
        - name: order
          resources:
            limits: 
              cpu: 500m
            requests:
              cpu: 200m
          image: 879772956301.dkr.ecr.eu-east-3.amazonaws.com/user19-order:v1
          ports:
            - containerPort: 8080
```

- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어준다.
```
kubectl get deploy order -w

kubectl get hpa order -w
```

- 사용자 2000명으로 워크로드를 3분이상 동안 걸어준다.
```
siege -r 2000 -c 200 -v -v 'http://a0c43786b71d549d2a02a45758b87b82-1426910919.ap-southeast-1.elb.amazonaws.com:8080/order/submitSurvey PATCH orderId=1&surveyResult=GOOD'


```

- 메트릭스 서버 설치 후 부하 테스트 진행하니 오토스케일 발생한 것을 확인하였음(레플리카 10)

![autoscale5](https://user-images.githubusercontent.com/81946287/120631690-1b336b00-c4a3-11eb-81cc-94d4e1ad1bb5.png)

***


## Zero-Downtime deploy (Readiness Probe)

- deployment.yml에 정상 적용되어 있는 readinessProbe  
```yml
readinessProbe:
  httpGet:
    path: '/actuator/health'
    port: 8080
  initialDelaySeconds: 10
  timeoutSeconds: 2
  periodSeconds: 5
  failureThreshold: 10
```

- deployment.yml에서 readiness 설정 제거 후, 배포중 siege 테스트 진행
- kubectl delete deploy 
- kubectl apply -f deployment.yml
- kubectl apply -f service.yaml

- readiness 적용 전. Order이 배포되는 중  
![image](https://user-images.githubusercontent.com/84304047/124864926-b9f83d80-dff4-11eb-9ace-ae6f425fe55b.png)

- 다시 readiness 정상 적용 후, Availability 100% 확인  
![image](https://user-images.githubusercontent.com/84304047/124998798-faed6200-e087-11eb-9707-1af31a75904b.png)

    
    
    
## Self-healing (Liveness Probe)

- deployment.yml에 정상 적용되어 있는 livenessProbe  

```yml
livenessProbe:
  httpGet:
    path: '/actuator/health'
    port: 8080
  initialDelaySeconds: 120
  timeoutSeconds: 2
  periodSeconds: 5
  failureThreshold: 5
```

- port 및 path 잘못된 값으로 변경 후, retry 시도 확인 
    - order 에 있는 deployment.yml 수정  
![image](https://user-images.githubusercontent.com/84304047/124913334-f7c38900-e029-11eb-8975-427ce3ee9b12.png)


    - retry 시도 확인 

![liveness](https://user-images.githubusercontent.com/81946287/120645682-aae11580-c4b3-11eb-94ba-1c757381e429.png)




