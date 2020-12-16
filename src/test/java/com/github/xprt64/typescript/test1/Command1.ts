import {CGeneric} from './CGeneric';
import {IGeneric} from './IGeneric';
import {ISimple} from './ISimple';
import {IGeneric2} from './IGeneric2';
import {CSimpleField} from './CSimpleField';
import {dispatchCommand} from '../../../../../api_delegate';

export interface Command1<A> extends CGeneric<IGeneric<number, string>, boolean>, ISimple, IGeneric2<A>
{
    int1: number;
    a: A;
    s1: string;
    cSimpleField1?: CSimpleField;
    intArray?: number[];
    intIntArray?: number[][];
    intIntIntArray?: number[][][];
    stringArray?: string[];
    genericArray?: IGeneric2<string>[];
    listString?: string[];
    collectinString?: string[];
    arrayOfListString?: string[][];
    ListArrayString?: string[][];
    listOfArrayInt?: number[][];
}

export async function dispatchCommand1(comanda: Command1) {
    return await dispatch(comanda)
}

export async function dispatch(comanda: Command1) {
    return await dispatchCommand("com.github.xprt64.typescript.test1.Command1", comanda)
}