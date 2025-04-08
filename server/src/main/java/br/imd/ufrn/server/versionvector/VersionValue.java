package br.imd.ufrn.server.versionvector;

public class VersionValue
{
    private String value;
    private VersionVector versionVector;

    public VersionValue(String value, VersionVector versionVector)
    {
        this.value = value;
        this.versionVector = versionVector;
    }

    public String getValue()
    {
        return value;
    }
}
