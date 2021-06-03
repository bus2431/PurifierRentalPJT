package purifierrentalpjt;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import purifierrentalpjt.config.kafka.KafkaProcessor;
import purifierrentalpjt.SurveyCompleted;

/**
 * 설문결과 View핸들러
 * @author Administrator
 *
 */
@Service
public class SurveyResultViewHandler {


    @Autowired
    private SurveyResultRepository surveyResultRepository;

    /**
     * 설문완료시
     * @param surveyCompleted
     */
    @StreamListener(KafkaProcessor.INPUT)
    public void when_surveyCompletionNotify (@Payload SurveyCompleted surveyCompleted) {
    	System.out.println("###SurveyResultViewHandler- 설문완료시");
    	
        try {
        	if( surveyCompleted.isMe()) {
	        	// view 객체 생성
	        	SurveyResult surveyResult = new SurveyResult();
	            surveyResult.setId		        (	surveyCompleted.getOrderId());
	            surveyResult.setSurveyResult	(	surveyCompleted.getSurveyResult());
	            surveyResultRepository.save(surveyResult);
        	}
        }catch (Exception e){
            e.printStackTrace();
        }
    }




}