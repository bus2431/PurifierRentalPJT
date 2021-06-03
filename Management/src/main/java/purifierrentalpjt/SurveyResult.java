package purifierrentalpjt;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="SurveyResult_table")
public class SurveyResult {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;
        private String surveyResult;	// 설문결과


        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getSurveyResult() {
            return surveyResult;
        }

        public void setSurveyResult(String surveyResult) {
            this.surveyResult = surveyResult;
        }

}
