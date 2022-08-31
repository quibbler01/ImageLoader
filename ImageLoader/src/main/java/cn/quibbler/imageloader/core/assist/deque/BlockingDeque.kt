package cn.quibbler.imageloader.core.assist.deque

import java.util.Deque
import java.util.concurrent.BlockingDeque

interface BlockingDeque<E> : BlockingDeque<E>, Deque<E> {

}