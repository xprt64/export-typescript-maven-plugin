package com.github.xprt64.typescript.test1;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class Command1<A> extends CGeneric<IGeneric<Integer, String>, Boolean> implements ISimple, IGeneric2<A>, Serializable {
    public final int int1 = 1;
    public final A a = null;
    private final String s1 = "";
    private CSimpleField cSimpleField1;
    public int[] intArray;
    public int[][] intIntArray;
    public int[][][] intIntIntArray;
    String[] stringArray;
    IGeneric2<String>[] genericArray;
    List<String> listString;
    Collection<String> collectinString;
    List<String>[] arrayOfListString;
    List<String[]> ListArrayString;
    List<int[]> listOfArrayInt;
}

interface ISimple {

}

interface IGeneric<A, B> {

}

interface IGeneric2<C> {

}

class CSimple {
    public int anInt;
    public Integer anInteger;
    public boolean aBoolean;
    public Boolean aBooleanClass;
}

class CGeneric<A, B> {
    public A anA;
    public B aB;
}

class CSimpleField{

}

enum SimpleEnum{
    VAL_A,
    VAL_B;
}