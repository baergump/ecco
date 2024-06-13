package result;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

public class Result {

    private int tp = 0;
    private int fp = 0;
    private int tn = 0;
    private int fn = 0;
    private double precision;
    private double recall;
    private double f1;
    private ResultContext resultContext;

    public int getTp(){ return this.tp; }
    public int getFp(){ return this.fp; }
    public int getTn(){ return this.tn; }
    public int getFn(){ return this.fn; }
    public double getPrecision(){ return this.precision;}
    public double getRecall(){ return this.recall; }
    public double getF1(){ return this.f1; }

    public void incTP(){this.tp++;}
    public void incFP(){this.fp++;}
    public void incTN(){this.tn++;}
    public void incFN(){this.fn++;}

    public void computeMetrics(){
        this.precision = (double) tp / (tp + fp);
        this.recall = (double) tp / (tp + fn);
        this.f1 = 2.0 * ((precision * recall) / (precision + recall));
    }

    public static Result overallResult(Collection<Result> results){
        Result overallResult = new Result();
        results.forEach(result -> overallResult.tp += result.tp);
        results.forEach(result -> overallResult.fp += result.fp);
        results.forEach(result -> overallResult.tn += result.tn);
        results.forEach(result -> overallResult.fn += result.fn);
        overallResult.computeMetrics();
        return overallResult;
    }

    public void save(Path path){
        String resultStart = "TP; FP; TN; FN; Precision; Recall; F1\n";
        String resultEnd = String.format("%d, %d, %d, %d, %f, %f, %f",
                this.tp, this.fp, this.tn, this.fn, this.precision, this.recall, this.f1);
        String resultString = resultStart + resultEnd;
        byte[] strToBytes = resultString.getBytes();
        try {
            Files.write(path.resolve("results.txt"), strToBytes);
        } catch (IOException e){
            throw new RuntimeException("Could not write result: " + e.getMessage());
        }
    }

    public void setResultContext(ResultContext resultContext){
        this.resultContext = resultContext;
    }

    public ResultContext getResultContext(){
        return this.resultContext;
    }
}
