import {Command} from "./com/cqrs/base/Command";
import {Question} from "./org/andrei/base/Question";

export async function dispatchCommand(commandType: string, command: Command) {

}

export async function answerQuestion<Q extends Question>(questionType: string, question: Q): Promise<Q> {

}