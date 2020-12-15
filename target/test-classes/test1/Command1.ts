import {dispatchCommand} from '../../../../../api_delegate';

export interface Command1 {
    int1?: number;
}

export async function dispatchCommand1(comanda: Command1) {
    return await dispatch(comanda)
}

export async function dispatch(comanda: Command1) {
    return await dispatchCommand("com.github.xprt64.typescript.test1.Command1", comanda)
}
