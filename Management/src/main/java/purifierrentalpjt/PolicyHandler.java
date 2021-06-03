package purifierrentalpjt;

import purifierrentalpjt.config.kafka.KafkaProcessor;
import purifierrentalpjt.SurveyCompleted;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired ManagementRepository managementRepository;

    /**
     * 설문이 완료됬을때 처리
     * @param surveyCompleted
     */
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverSurveyCompleted_SurveyCompletionNotify(@Payload SurveyCompleted surveyCompleted){

        if(!surveyCompleted.validate()) return;

        System.out.println("\n\n##### listener SurveyCompletionNotify : " + surveyCompleted.toJson() + "\n\n");

        try {
            managementRepository.findById(surveyCompleted.getOrderId()).ifPresent(
                management -> {
                    management.setStatus("surveyComplete");
                    managementRepository.save(management);
            });
        } catch(Exception e) {
            e.printStackTrace();
        }
            
    }



    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
