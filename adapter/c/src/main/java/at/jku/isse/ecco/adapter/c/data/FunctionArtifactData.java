package at.jku.isse.ecco.adapter.c.data;

public class FunctionArtifactData {

    private String signature;

    public FunctionArtifactData(String signature){
        this.signature = signature;
    }

    public void setSignature(String signature){
        this.signature = signature;
    }

    public String getSignature(){
        return this.signature;
    }
}
