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
    - [Circuit Breaker](#Circuit-Breaker)
    - [Zero-Downtime deploy(Readiness Probe)](#Zero-Downtime-deploy(Readiness-Probe))
    - [Self-healing(Liveness Probe)](#Self-healing)
  - [신규 개발 조직의 추가](#신규-개발-조직의-추가)

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
1. 가입신청이 완료되면 고객이 만족도 응답을 할 수 있다.
2. 설문이 제출되면 설문이 종료된다.(설문종료 처리는 Req/Res 테스트를 위해 임의로 동기처리)
3. 설문완료된 상태값과 설문결과를 고객이 수시로 확인할 수 있다.

비기능적 요구사항
1. 트랜잭션
    1. 가입취소 신청은 설치취소가 동시 이루어 지도록 한다
    2. (개인과제)설문 종료는 설문 제출과 동시에 이루어지도록 한다.
    
1. 장애격리
    1. 공기청정기 렌탈 가입신청과 취소는 고객서비스 담당자의 접수, 설치 처리와 관계없이 항상 처리 가능하다. 
    2. 가입처리는 설문관리와 관계없이 항상 처리 가능하다.

1. 성능
    1. 고객은 주문/설치 진행상태를 수시로 확인한다.(CQRS)
    2. (개인과제)고객은 설문결과도 수시로 확인한다.(CQRS)



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
![image](https://user-images.githubusercontent.com/84304047/124766408-dacb7f00-df71-11eb-8101-a3d02053ef51.png)


### 서비스 추가된 완성본에 대한 기능적/비기능적 요구사항을 커버하는지 검증
#### 시나리오 Coverage Check
![image](https://user-images.githubusercontent.com/84304047/124768850-f899e380-df73-11eb-8c8e-0443474bba34.png)


# 구현:
분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 8084 이다)

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
1) 공기청정기 렌탈 서비스 가입완료 후 설문조사 처리

- (a) http -f POST  http://localhost:8081/order/joinOrder productId=1 productName=PURI1 installationAddress="Addr1" customerId=101
- (b) http -f PATCH http://localhost:8083/installations orderId=1 
- (c) http -f PATCH http://localhost:8081/order/submitSurvey orderId=1 surveyResult="GOOD"
![Survey_command](https://user-images.githubusercontent.com/81946287/120572864-a7b83c00-c457-11eb-8254-6237c680da5e.png)

2) 카프카 메시지 확인

- (a) 설문조사 제출 후 : surveySubmit
![Survey_kafka](https://user-images.githubusercontent.com/81946287/120572892-b56dc180-c457-11eb-990f-49f8578e9994.png)




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
Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능하게 구현해 두었다.
본 프로젝트에서  역할은 view 서비스가 수행한다.

모든 정보는 비동기 방식으로 발행된 이벤트(예매, 예매 취소)를 수신하여 처리된다.

예매(Booked) 실행
 
![image](https://user-images.githubusercontent.com/85874443/122846091-17776380-d340-11eb-87e6-fb330d787236.PNG)

카프카 메시지

![ka1](https://user-images.githubusercontent.com/85874443/122853258-e6516000-d34c-11eb-9783-37814741be1c.PNG)

예매(Booked) 실행 후 mypage 화면

![image](https://user-images.githubusercontent.com/85874443/122846131-2a8a3380-d340-11eb-851e-be9df34e5cdf.PNG)
  
## 폴리글랏 퍼시스턴스
concert 서비스의 DB 를 HSQL 로 설정하여 MSA간 서로 다른 종류의 DB간에도 문제 없이 동작하여 다형성을 만족하는지 확인하였다.

|서비스|DB|pom.xml|
| :--: | :--: | :--: |
|Management| HSQL |![image](https://user-images.githubusercontent.com/84304047/124858788-b1e6d080-dfe9-11eb-9547-4c98cad2b794.png)|
|Assignment| H2 |![image](https://user-images.githubusercontent.com/84304047/124858681-7cda7e00-dfe9-11eb-991c-2cfde83cf5da.png)|
|Order| H2 |![image](https://user-images.githubusercontent.com/84304047/124858717-89f76d00-dfe9-11eb-815d-b1de020a0581.png)|


## 동기식 호출과 Fallback 처리
분석단계에서의 조건 중 하나로  콘서트 티켓 예약수량은 등록된 티켓 수량을 초과 할 수 없으며
예약(Booking)->콘서트(Concert) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 
호출 프로토콜은 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.



Booking  내 external.ConcertService

```java
package concertbooking.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(name="Concert", url="http://localhost:8081")
public interface ConcertService {

    @RequestMapping(method= RequestMethod.GET, path="/checkAndBookStock")
    public boolean checkAndBookStock(@RequestParam("ccId") Long ccId , @RequestParam("qty") int qty);

}
```

Booking 서비스 내 Req/Resp

```java
    @PostPersist
    public void onPostPersist() throws Exception{
        
        
        boolean rslt = BookingApplication.applicationContext.getBean(concertbooking.external.ConcertService.class)
            .checkAndBookStock(this.getCcId(), this.getQty());

            if (rslt) {
                Booked booked = new Booked();
                BeanUtils.copyProperties(this, booked);
                booked.publishAfterCommit();
            }  
            else{
                throw new Exception("Out of Stock Exception Raised.");
            }      
        

    }
```

Concert 서비스 내 Booking 서비스 Feign Client 요청 대상

```java
@RestController
public class ConcertController {

@Autowired
ConcertRepository concertRepository;

@RequestMapping(value = "/checkAndBookStock",
        method = RequestMethod.GET,
        produces = "application/json;charset=UTF-8")

public boolean checkAndBookStock(HttpServletRequest request, HttpServletResponse response)
        throws Exception {
     
        System.out.println("##### /concert/checkAndBookStock  called #####");

        boolean status = false;
        
        Long ccId = Long.valueOf(request.getParameter("ccId"));
        int qty = Integer.parseInt(request.getParameter("qty"));

        System.out.println("##### ccid #####" + ccId +"##### qty" + qty);
        Optional<Concert> concert = concertRepository.findById(ccId);
        
        if(concert.isPresent()){

                Concert concertValue = concert.get();

                if (concertValue.getStock() >= qty) {
                        concertValue.setStock(concertValue.getStock() - qty);
                        concertRepository.save(concertValue);
                        status = true;
                        System.out.println("##### /concert/checkAndBookStock  qty check true ##### stock"+concertValue.getStock()+"### qty"+ qty);
                }

                System.out.println("##### /concert/checkAndBookStock  qty check false ##### stock"+concertValue.getStock()+"### qty"+ qty);
        }

        return status;
        }
        
 }
```

공연 정보를 등록함

![concert](https://user-images.githubusercontent.com/85874443/122849383-61634800-d346-11eb-8d6d-73c09867dc17.PNG)



티켓을 예매함
![booking](https://user-images.githubusercontent.com/85874443/122849272-252fe780-d346-11eb-8ee5-51469a470115.PNG)


티켓 예매를 취소함
![cancle](https://user-images.githubusercontent.com/85874443/122849246-1a755280-d346-11eb-9455-e7a4de36cf12.PNG)


# 운영

## Deploy/Pipeline
각 구현체들은 각자의 source repository 에 구성되었고, 각 서비스별로 빌드를 하여, aws ecr에 등록 후 deployment.yaml 통해 EKS에 배포함.

- git에서 소스 가져오기

```
git clone --recurse-submodules https://github.com/skteam4/concert/concertbooking.git
```

- Build 하기

```bash
cd /alarm
cd gateway
mvn package

cd ..
cd booking
mvn package

cd ..
cd concert
mvn package

cd ..
cd delivery
mvn package

cd ..
cd payment
mvn package
```

- aws 이미지 캡처

<img width="705" alt="aws_repository" src="https://user-images.githubusercontent.com/85874443/122850409-1f3b0600-d348-11eb-8ebd-e3653bafe919.PNG">


<img width="682" alt="aws_book_tag" src="https://user-images.githubusercontent.com/85874443/122850413-2235f680-d348-11eb-807f-b2aef08c24ff.PNG">


- concert/booking/kubernetes/deployment.yml 파일 

```yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: booking
  labels:
    app: booking
spec:
  replicas: 1
  selector:
    matchLabels:
      app: booking
  template:
    metadata:
      labels:
        app: booking
    spec:
      containers:
        - name: booking
          image: xxxxxx.dkr.ecr.ca-central-1.amazonaws.com/booking:v4
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
```	  


***



## Autoscale

- metric 서버를 설치한다.

```sh
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.3.7/components.yaml
kubectl get deployment metrics-server -n kube-system
```

- 예약 서비스에 리소스에 대한 사용량을 정의한다.

<code>booking/kubernetes/deployment.yml</code>

```yml
  resources:
    requests:
      memory: "64Mi"
      cpu: "250m"
    limits:
      memory: "500Mi"
      cpu: "500m"
```

- 예약 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 20프로를 넘어서면 replica 를 3개까지 늘려준다:

```sh
$ kubectl autoscale deploy booking --min=1 --max=3 --cpu-percent=20
```

- CB 에서 했던 방식대로 워크로드를 걸어준다.

```sh
siege -c20 -t40S -v --content-type "application/json" 'http://localhost:8082/bookings POST {“ccId”:"1", "ccName":"mong", "ccDate:"20210621", “qty”:”2" ,”customerId”:"6007" , "bookingStatus":"success"}'
```

- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다:

```sh
$ kubectl get deploy booking -w
```

- 어느정도 시간이 흐른 후 스케일 아웃이 벌어지는 것을 확인할 수 있다:

* siege 부하테스트 - 후 1

![hpa](https://user-images.githubusercontent.com/85874443/122758180-76eb5a00-d2d3-11eb-9618-e2005145b0de.PNG)


* siege 부하테스트 - 후 2


![scaleout_최종](https://user-images.githubusercontent.com/85874443/122758323-a13d1780-d2d3-11eb-8687-fc39ef7008a5.PNG)


## Circuit Breaker

  * 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 설치
  * 시나리오는 예약(booking) >> 콘서트(concert) 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 예약 요청이 과도할 경우 CB 를 통하여 장애격리
  * Booking 서비스 내 XX에 FeignClient 에 적용
  * Hystrix 설정

```yml
# application.yml

feign:
  hystrix:
    enabled: true

hystrix:
  command:
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610
```

- 부하 테스트 수행
```sh
$ siege -c20 -t40S -v --content-type "application/json" 'http://localhost:8082/bookings POST {"ccId":1, "ccName":"mong", "ccDate":"20210621", "qty":2 ,"customerId":6007 ,"bookingStatus":"success"}'
```

- fallback 설정

![fallback설정](https://user-images.githubusercontent.com/85874443/122866266-9d0c0b00-d362-11eb-92ca-43179c843e30.PNG)
![fallback함수](https://user-images.githubusercontent.com/85874443/122866315-b4e38f00-d362-11eb-8437-dd24f46977eb.PNG)


- Hystrix 설정 + fallback 설정 전

  ![Hystrix설정후_fallback설정전](https://user-images.githubusercontent.com/85874443/122845849-899b7880-d33f-11eb-8f9b-e266db0afde1.PNG)

  
- Hystrix 설정 + fallback 설정 후

  ![Hystrix설정전_fallback설정후](https://user-images.githubusercontent.com/85874443/122845630-172a9880-d33f-11eb-9aec-5592f9a56ee3.PNG)

- 부하를 줬을 때 fallback 설정 전에는 500 에러가 발생했으나, fallback 설정 이후에는 100% 정상적으로 처리함

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
- kubectl delete deploy --all
- kubectl apply -f deployment.yml
- kubectl apply -f service.yaml

- readiness 적용 전. booking이 배포되는 중  
  ![update_version_80%](https://user-images.githubusercontent.com/85874443/122764789-c84b1780-d2da-11eb-951c-b6058f77b208.PNG)


- 다시 readiness 정상 적용 후, Availability 100% 확인  
  ![update_version_100%](https://user-images.githubusercontent.com/85874443/122764804-ce40f880-d2da-11eb-83fa-af8a85d8431b.PNG)


    
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
    - booking 에 있는 deployment.yml 수정  
        ![livenessProbe_yaml](https://user-images.githubusercontent.com/85874443/122760461-1c073200-d2d6-11eb-8db8-c25c6ef9abb4.png)


    - retry 시도 확인  
        ![livenessProbe](https://user-images.githubusercontent.com/85874443/122760301-ecf0c080-d2d5-11eb-9da5-bd39c7867e24.png)
































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
1) 공기청정기 렌탈 서비스 가입완료 후 설문조사 처리

- (a) http -f POST  http://localhost:8081/order/joinOrder productId=1 productName=PURI1 installationAddress="Addr1" customerId=101
- (b) http -f PATCH http://localhost:8083/installations orderId=1 
- (c) http -f PATCH http://localhost:8081/order/submitSurvey orderId=1 surveyResult="GOOD"
![Survey_command](https://user-images.githubusercontent.com/81946287/120572864-a7b83c00-c457-11eb-8254-6237c680da5e.png)

2) 카프카 메시지 확인

- (a) 설문조사 제출 후 : surveySubmit
![Survey_kafka](https://user-images.githubusercontent.com/81946287/120572892-b56dc180-c457-11eb-990f-49f8578e9994.png)


## 폴리글랏 퍼시스턴스
- order, Assignment, installation 서비스 모두 H2 메모리DB를 적용하였다.  
- 로컬환경에서 Management 서비스에 대해서 MongoDB를 적용하였다
다양한 데이터소스 유형 (RDB or NoSQL) 적용 시 데이터 객체에 @Entity 가 아닌 @Document로 마킹 후, 기존의 Entity Pattern / Repository Pattern 적용과 데이터베이스 제품의 설정 (application.yml) 만으로 가능하다.

```
--application.yml 
spring:
  profiles: default
  jpa:
    properties:
      hibernate:
        show_sql: true
        format_sql: true
  cloud:
    stream:
      kafka:
        binder:
          brokers: localhost:9092
        streams:
          binder:
            configuration:
              default:
                key:
                  serde: org.apache.kafka.common.serialization.Serdes$StringSerde
                value:
                  serde: org.apache.kafka.common.serialization.Serdes$StringSerde
      bindings:
        event-in:
          group: Management
          destination: purifierrentalpjt
          contentType: application/json
        event-out:
          destination: purifierrentalpjt
          contentType: application/json
  data:
    mongodb:
      uri: mongodb://localhost:27017/testdb
```

- Repository 설정
```
package purifierrentalpjt;

import org.springframework.data.repository.CrudRepository;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="product", path="product")
//public interface ManagementRepository extends CrudRepository<Management, Long>{

public interface ManagementRepository extends MongoRepository<Management, Long>{ 
    Optional<Management> findByOrderId(Long orderId);

}
```

- Management.java
```
import org.springframework.data.mongodb.core.mapping.Document;

//@Entity
//@Table(name="Management_table")
@Document(collection = "SURVEY")
public class Management {

    @Id
    //@GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long orderId;
    private String surveyResult;

    @PostPersist
    public void onPostPersist(){

        System.out.println("=================>>>>" + this.getStatus() + "POST TEST");

        SurveyCompleted surveyCompleted = new SurveyCompleted();

        surveyCompleted.setId(this.getId());
        surveyCompleted.setOrderId(this.orderId);
        surveyCompleted.setSurveyResult(this.getSurveyResult());
        BeanUtils.copyProperties(this, surveyCompleted);
        surveyCompleted.publishAfterCommit();


    }
```




## 동기식 호출 과 Fallback 처리

- 분석 단계에서의 조건 중 하나로 주문(Order) 서비스에서 설문 제출 요청 받으면, 
관리(management) 서비스 설문종료 처리하는 부분을 동기식 호출하는 트랜잭션으로 처리하기로 하였다. 
- 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어 있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.

관리 서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현
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
   		 public void completeSurvey(@RequestBody Management management);

	}
```

설문이 제출되면(@PostUpdate) 설문 완료 처리가 되도록 처리
```
# (Order) Order.java

    @PostUpdate
    public void onPostUpdate(){
        /* 설문조사 */
    	System.out.println("### 설문 상태 Update and Update Event raised..." + this.getStatus());
        if(this.getStatus().equals("surveySubmit")) {
            SurveySubmitted surveySubmitted = new SurveySubmitted();
            BeanUtils.copyProperties(this, surveySubmitted);
            surveySubmitted.publishAfterCommit();

            purifierrentalpjt.external.Management management = new purifierrentalpjt.external.Management();

            management.setId(this.getId());

            OrderApplication.applicationContext.getBean(purifierrentalpjt.external.ManagementService.class)
            .completeSurvey(management);
        }


    }
   
```

동기식 호출에서는 호출 시간에 따른 타입 커플링이 발생하며, 관리(Management) 서비스가 장애가 나면 설문이 제출되지 않는다는 것을 확인
![동기호출](https://user-images.githubusercontent.com/81946287/120578039-2022fb00-c460-11eb-8156-dc6aaed13bf4.png)

## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트

설문완료(Management)가 이루어진 후에 주문(Order) 서비스로 이를 알려주는 행위는 비동기식으로 처리하였다.
 
- 이를 위하여 설문완료 후 곧바로 설문조사가 완료되었다는 도메인 이벤트를 카프카로 송출한다.(Publish)
```
# (Management) Management.java

    @PostPersist
    public void onPostPersist(){

        System.out.println("=================>>>>" + this.getStatus() + "POST TEST");

        SurveyCompleted surveyCompleted = new SurveyCompleted();

        surveyCompleted.setId(this.getId());
        surveyCompleted.setOrderId(this.orderId);
        surveyCompleted.setStatus(this.getStatus()); 
        surveyCompleted.setSurveyResult(this.getSurveyResult());
        BeanUtils.copyProperties(this, surveyCompleted);
        surveyCompleted.publishAfterCommit();


    }
```
- 주문 서비스에서는 설문 완료 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다.
```
# (Order) PolicyHandler.java

@Service
public class PolicyHandler{
    @Autowired OrderRepository orderRepository;

    /**
     * 설문이 완료됬을때 처리
     * @param surveyCompleted
     */
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverSurveyCompleted_SurveyCompletionNotify(@Payload SurveyCompleted surveyCompleted){

        if(!surveyCompleted.validate()) return;

        System.out.println("\n\n##### listener SurveyCompletionNotify : " + surveyCompleted.toJson() + "\n\n");

        try {
            orderRepository.findById(surveyCompleted.getOrderId()).ifPresent(
                order -> {
                    order.setStatus("surveyComplete");
                    order.setSurveyResult(surveyCompleted.getSurveyResult());
                    orderRepository.save(order);
            });
        } catch(Exception e) {
            e.printStackTrace();
        }
            
    }
}
```
관리의 설문완료는 주문 서비스와 완전히 분리되어 있으며, 이벤트 수신에 따라 처리되기 때문에, 주문 서비스가 유지보수로 인해 잠시 내려간 상태라도 
설문완료 처리를 하는데 문제가 없다.


## CQRS

가입신청+설문진행 상태 조회를 위한 서비스를 CQRS 패턴으로 구현하였다.
- Order, Assignment, Installation, Management 개별 aggregate 통합 조회로 인한 성능 저하를 막을 수 있다.
- 모든 정보는 비동기 방식으로 발행된 이벤트를 수신하여 처리된다.
- 설계 : MSAEz 설계의 view 매핑 설정 참조

- 설문 제출

![설문](https://user-images.githubusercontent.com/81946287/120587902-2d94b100-c471-11eb-9647-4a27811fdccb.png)

- 카프카 메시지

![설문_kafka](https://user-images.githubusercontent.com/81946287/120587906-2ff70b00-c471-11eb-92ac-274ea581b944.png)


- 뷰테이블 수신처리

![ViewHandler](https://user-images.githubusercontent.com/81946287/120587912-32596500-c471-11eb-8f64-1ee819ddfc25.png)

![ViewHandler2](https://user-images.githubusercontent.com/81946287/120587921-35545580-c471-11eb-86c2-741a5ae8455e.png)




# 운영

## CI/CD 설정
### 빌드/배포
각 프로젝트 jar를 Dockerfile을 통해 Docker Image 만들어 ECR저장소에 올린다.   
EKS 클러스터에 접속한 뒤, 각 서비스의 deployment.yaml, service.yaml을 kuectl명령어로 서비스를 배포한다.   
  - 코드 형상관리 : https://github.com/llyyjj99/PurifierRentalPJT 하위 repository에 각각 구성   
  - 운영 플랫폼 : AWS의 EKS(Elastic Kubernetes Service)   
  - Docker Image 저장소 : AWS의 ECR(Elastic Container Registry)
##### 배포 명령어
```
$ kubectl apply -f deployment.yml
$ kubectl apply -f service.yaml
```

##### 배포 결과
![배포결과](https://user-images.githubusercontent.com/81946287/120602799-570c0780-c486-11eb-932c-f6577eb6ee65.png)

## 동기식 호출 / 서킷 브레이킹 / 장애격리

* 서킷 브레이킹 프레임워크의 선택
  - Spring FeignClient + Hystrix 옵션을 사용하여 구현할 경우, 도메인 로직과 부가 기능 로직이 서비스에 같이 구현된다.
  - istio를 사용해서 서킷 브레이킹 적용이 가능하다.

- istio 설치

![istio](https://user-images.githubusercontent.com/81946287/120603043-8fabe100-c486-11eb-9141-c60371128868.png)                                                                                                              


- istio 에서 서킷브레이커 설정(DestinationRule)
```
cat <<EOF | kubectl apply -f -
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: order
spec:
  host: order
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 1           # 목적지로 가는 HTTP, TCP connection 최대 값. (Default 1024)
      http:
        http1MaxPendingRequests: 1  # 연결을 기다리는 request 수를 1개로 제한 (Default 
        maxRequestsPerConnection: 1 # keep alive 기능 disable
        maxRetries: 3               # 기다리는 동안 최대 재시도 수(Default 1024)
    outlierDetection:
      consecutiveErrors: 5          # 5xx 에러가 5번 발생하면
      interval: 1s                  # 1초마다 스캔 하여
      baseEjectionTime: 30s         # 30 초 동안 circuit breaking 처리   
      maxEjectionPercent: 100       # 100% 로 차단
EOF

```

* 부하테스터 siege 툴을 통한 서킷 브레이커 동작을 확인한다.
- 동시사용자 300명
- 100초 동안 실시
- siege -c300 -t100S  -v 'http://ad9fa9f229b5b4d51b2bfb0e4dab31ba-1730418607.ap-southeast-1.elb.amazonaws.com:8080/order/submitSurvey PATCH orderId=1&surveyResult=GOOD'
- 결과 화면
![cb](https://user-images.githubusercontent.com/81946287/120654866-0a8fee80-c4bd-11eb-9e53-d62079fb0981.png)


### Liveness
pod의 container가 정상적으로 기동되는지 확인하여, 비정상 상태인 경우 pod를 재기동하도록 한다.   

아래의 값으로 liveness를 설정한다.
- 재기동 제어값 : /tmp/healthy 파일의 존재를 확인
- 기동 대기 시간 : 3초
- 재기동 횟수 : 5번까지 재시도

이때, 재기동 제어값인 /tmp/healthy파일을 강제로 지워 liveness가 pod를 비정상 상태라고 판단하도록 하였다.    
5번 재시도 후에도 파드가 뜨지 않았을 경우 CrashLoopBackOff 상태가 됨을 확인하였다.   
##### order에 Liveness 적용한 내용
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order
  labels:
    app: order
spec:
    spec:
      containers:
        - name: order
          image: 879772956301.dkr.ecr.ap-southeast-1.amazonaws.com/puri12-order:v1
          args:
          - /bin/sh
          - -c
          - touch /tmp/healthy; sleep 10; rm -rf /tmp/healthy; sleep 600;
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
            exec:
              command:
              - cat
              - /tmp/healthy
            initialDelaySeconds: 3
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
```


- 확인 : kubectl get pods -w

![liveness](https://user-images.githubusercontent.com/81946287/120645682-aae11580-c4b3-11eb-94ba-1c757381e429.png)


### 오토스케일 아웃

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
          image: 879772956301.dkr.ecr.ap-southeast-1.amazonaws.com/puri12-order:v1
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

- 메트릭스 서버 설치 후 부하 테스트 진행하니 오토스케일 발생한 것을 확인하였음

![autoscale3](https://user-images.githubusercontent.com/81946287/120631667-179fe400-c4a3-11eb-8004-013536c07f27.png)

![autoscale5](https://user-images.githubusercontent.com/81946287/120631690-1b336b00-c4a3-11eb-81cc-94d4e1ad1bb5.png)

## 무정지 재배포

* 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 서킷브레이커 설정을 제거함

- seige 로 배포작업 직전에 워크로드를 모니터링 한다.
```
siege -c250 -t200S  -v 'http://ad9fa9f229b5b4d51b2bfb0e4dab31ba-1730418607.ap-southeast-1.elb.amazonaws.com:8080/order/submitSurvey PATCH orderId=1&surveyResult=BAD'
```

- readinessProbe, livenessProbe 설정되지 않은 상태
- siege 수행 결과 : 
![readness](https://user-images.githubusercontent.com/81946287/120746953-2b972480-c53b-11eb-8ce4-8d285628e149.png)

- readinessProbe, livenessProbe 설정한 상태
- siege 수행 결과 : 
![readness](https://user-images.githubusercontent.com/81946287/120744113-45356d80-c535-11eb-940f-c97d3b7fc848.png)



## 운영 모니터링

### 쿠버네티스 구조
쿠버네티스는 Master Node(Control Plane)와 Worker Node로 구성된다.

![image](https://user-images.githubusercontent.com/64656963/86503139-09a29880-bde6-11ea-8706-1bba1f24d22d.png)


### 1. Master Node(Control Plane) 모니터링
Amazon EKS 제어 플레인 모니터링/로깅은 Amazon EKS 제어 플레인에서 계정의 CloudWatch Logs로 감사 및 진단 로그를 직접 제공한다.

- 사용할 수 있는 클러스터 제어 플레인 로그 유형은 다음과 같다.
```
  - Kubernetes API 서버 컴포넌트 로그(api)
  - 감사(audit) 
  - 인증자(authenticator) 
  - 컨트롤러 관리자(controllerManager)
  - 스케줄러(scheduler)

출처 : https://docs.aws.amazon.com/ko_kr/eks/latest/userguide/logging-monitoring.html
```

- 제어 플레인 로그 활성화 및 비활성화
```
기본적으로 클러스터 제어 플레인 로그는 CloudWatch Logs로 전송되지 않습니다. 
클러스터에 대해 로그를 전송하려면 각 로그 유형을 개별적으로 활성화해야 합니다. 
CloudWatch Logs 수집, 아카이브 스토리지 및 데이터 스캔 요금이 활성화된 제어 플레인 로그에 적용됩니다.

출처 : https://docs.aws.amazon.com/ko_kr/eks/latest/userguide/control-plane-logs.html
```

### 2. Worker Node 모니터링

- 쿠버네티스 모니터링 솔루션 중에 가장 인기 많은 것은 Heapster와 Prometheus 이다.
- Heapster는 쿠버네티스에서 기본적으로 제공이 되며, 클러스터 내의 모니터링과 이벤트 데이터를 수집한다.
- Prometheus는 CNCF에 의해 제공이 되며, 쿠버네티스의 각 다른 객체와 구성으로부터 리소스 사용을 수집할 수 있다.

- 쿠버네티스에서 로그를 수집하는 가장 흔한 방법은 fluentd를 사용하는 Elasticsearch 이며, fluentd는 node에서 에이전트로 작동하며 커스텀 설정이 가능하다.

- 그 외 오픈소스를 활용하여 Worker Node 모니터링이 가능하다. 아래는 istio, mixer, grafana, kiali를 사용한 예이다.

```
아래 내용 출처: https://bcho.tistory.com/1296?category=731548

```
- 마이크로 서비스에서 문제점중의 하나는 서비스가 많아 지면서 어떤 서비스가 어떤 서비스를 부르는지 의존성을 알기가 어렵고, 각 서비스를 개별적으로 모니터링 하기가 어렵다는 문제가 있다. Istio는 네트워크 트래픽을 모니터링함으로써, 서비스간에 호출 관계가 어떻게 되고, 서비스의 응답 시간, 처리량등의 다양한 지표를 수집하여 모니터링할 수 있다.

![image](https://user-images.githubusercontent.com/64656963/86347967-ff738380-bc99-11ea-9b5e-6fb94dd4107a.png)

- 서비스 A가 서비스 B를 호출할때 호출 트래픽은 각각의 envoy 프록시를 통하게 되고, 호출을 할때, 응답 시간과 서비스의 처리량이 Mixer로 전달된다. 전달된 각종 지표는 Mixer에 연결된 Logging Backend에 저장된다.

- Mixer는 위의 그림과 같이 플러그인이 가능한 아답터 구조로, 운영하는 인프라에 맞춰서 로깅 및 모니터링 시스템을 손쉽게 변환이 가능하다.  쿠버네티스에서 많이 사용되는 Heapster나 Prometheus에서 부터 구글 클라우드의 StackDriver 그리고, 전문 모니터링 서비스인 Datadog 등으로 저장이 가능하다.

![image](https://user-images.githubusercontent.com/64656963/86348023-14501700-bc9a-11ea-9759-a40679a6a61b.png)

- 이렇게 저장된 지표들은 여러 시각화 도구를 이용해서 시각화 될 수 있는데, 아래 그림은 Grafana를 이용해서 서비스의 지표를 시각화 한 그림이다.

![image](https://user-images.githubusercontent.com/64656963/86348092-25992380-bc9a-11ea-9d7b-8a7cdedc11fc.png)

- 그리고 근래에 소개된 오픈소스 중에서 흥미로운 오픈 소스중의 하나가 Kiali (https://www.kiali.io/)라는 오픈소스인데, Istio에 의해서 수집된 각종 지표를 기반으로, 서비스간의 관계를 아래 그림과 같이 시각화하여 나타낼 수 있다.  아래는 그림이라서 움직이는 모습이 보이지 않지만 실제로 트래픽이 흘러가는 경로로 에니메이션을 이용하여 표현하고 있고, 서비스의 각종 지표, 처리량, 정상 여부, 응답 시간등을 손쉽게 표현해 준다.

![image](https://user-images.githubusercontent.com/64656963/86348145-3a75b700-bc9a-11ea-8477-e7e7178c51fe.png)


