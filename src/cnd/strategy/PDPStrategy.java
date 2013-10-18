package cnd.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.sun.tools.javac.code.Type.ForAll;

import util.StringUtil;

import cnd.CndResult;

public class PDPStrategy implements CndStrategy {

	/**
	 * 分散の上限
	 * 理想はプロセッサ数だと思う
	 */
	private final static int THREAD_MAX_SIZE = 10;
//	private final static int THREAD_MAX_SIZE = Runtime.getRuntime().availableProcessors();
	
	/** 引数Lの最小値 **/
	private final static int LENGTH_MIN = 2;
	/** 引数Lの最大値(独断と偏見) **/
	private final static int LENGTH_MAX = 10000;
	
	
	enum State {
		Success(1, "正常終了"),
		NoAnser(0, "正常終了(解なし)"),
		LengthMinError(-1, "引数の値が小さすぎます。" + LENGTH_MIN + "が最小値です。"),
		LengthMaxError(-2, "引数の値が大きすぎます。" + LENGTH_MAX + "まで指定可能です。")
		;
		private int stateCode;
		private String message;
		private State(int stateCode, String message) {
			this.stateCode = stateCode;
			this.message = message;
		}
		public String message() {
			return message;
		}
		public boolean isError() {
			return (stateCode < 0 ? true : false);
		}
	}
	
	private static class Task implements Callable<List<List<Integer>>> {
		private int max;
		private int count;
		private List<List<Integer>> result;
		public Task(int max, int count) {
			this.max = max;
			this.count = count;
			this.result = new ArrayList<List<Integer>>();
		}
		
		public List<List<Integer>> call() throws Exception {
			System.out.println("Task: "+max+", "+count);
			List<Integer> work = new LinkedList<Integer>();
			for (int i = 0; i <= max; i++) {
				work.add(i);
			}
			_combination(0, max, count, work, result);
			return result;
		}
		
		private void _combination(int startIndex, int max, int count, List<Integer> work, List<List<Integer>> result)
		{
			if(startIndex==count){
				List<Integer> numberList = new LinkedList<Integer>();
				for (int i = 1; i <= count; i++){
					numberList.add(work.get(i));
				}
				result.add(numberList);
			}
			else {
				//桁数を増す
				int w = work.get(startIndex+1);
				for (int i = startIndex + 1; i <= max; i++){
					work.set(startIndex+1, work.get(i));//固定する数
					work.set(i, w);
					_combination(startIndex+1, max, count, work, result);//順列の場合
//					if (work.get(startIndex) < work.get(startIndex+1)) {
//						_combination(startIndex+1, max, count, work, result);//組み合わせの場合
//					}
					work.set(i, work.get(startIndex+1));
				}
				work.set(startIndex+1, w);
			}
		}
	}
	
	/** 引数L **/
	private int length;
	/** 組み合わせのパターン数 **/
	private int totalPattern;
	/** 解のリスト **/
	private List<CndResult> result;
	/** タスク管理 **/
	private ExecutorService pool;
	
	public PDPStrategy(int length) {
		this.length = length;
		this.result = Collections.synchronizedList(new ArrayList<CndResult>());
		this.pool = Executors.newFixedThreadPool(THREAD_MAX_SIZE);
	}
	
	@Override
	public String calculate() {
		
		State state = checkError();
		if (state != State.Success) {
			return resultToString(state);
		}
		
		// パターン数を求める
		totalPattern = calcTotalPattern();
		
		// 循環リストのパターンを取得
		// 1. 先頭は必ず1
		// 2. 2番目の要素 < 末尾の要素
		List<List<Integer>> numberCirclPattern = numberCombination(length);
		List<List<Integer>> deleteList = new ArrayList<List<Integer>>();
		for (Iterator<List<Integer>> it = numberCirclPattern.iterator(); it.hasNext();) {
			List<Integer> numberList = it.next();
			if (numberList.get(0) != 1) {
				deleteList.add(numberList);
			} else if (numberList.get(1) > numberList.get(numberList.size()-1)) {
				deleteList.add(numberList);
			}
		}
		numberCirclPattern.removeAll(deleteList);
//		System.out.println(numberCirclPattern);
		
		// 足しあわせのパターンをすべて求める
		for (List<Integer> numberCirclList : numberCirclPattern) {
			List<List<Integer>> numberPatternList = parse(length, numberCirclList);
			LinkedList<Integer> totalNumberList = new LinkedList<Integer>();
			List<String> patternList = new ArrayList<String>();
			
			for (List<Integer> numberList : numberPatternList) {
				int sum = 0;
				StringBuilder sb = new StringBuilder();
				for (Integer number : numberList) {
					if (sum != 0) {
						sb.append(" + ");
					}
					sum += number;
					sb.append(number);
				}
				sb.append(" = ").append(sum);
				totalNumberList.add(sum);
				patternList.add(sb.toString());
			}
			
			// 総数チェック
			if (totalPattern != patternList.size()) {
				patternList.add("総パターン数と一致しませんでした");
			}
			
			result.add(new CndResult(numberCirclList, totalNumberList, patternList));
		}
		
		return resultToString(State.Success);
	}
	
	private State checkError() {
		// 2より小さい場合エラーとする
		if (length < LENGTH_MIN) {
			return State.LengthMinError;
		}

		// 処理時間やメモリの理由でとりあえず最大値を設けておく
		if (length >= LENGTH_MAX) {
			return State.LengthMaxError;
		}
		
		return State.Success;
	}
	
	/**
	 * パターン数を求める
	 * n(L) = L(L - 1) + 1
	 * @return
	 */
	private int calcTotalPattern() {
		return (length * (length - 1)) + 1;
	}
	
	/**
	 * 連番のリストを出力
	 * @return
	 */
	private LinkedList<Integer> createCirclList() {
		LinkedList<Integer> numberCirclList = new LinkedList<Integer>();
		for (int i = 1; i <= length; i++) {
			numberCirclList.add(i);
		}
		return numberCirclList;
	}
	
	/**
	 * 循環リストの切り取る位置を固定して、1～maxそれぞれの長さで切り取る
	 * @param max
	 * @return
	 */
	public List<List<Integer>> parse(int max, List<Integer> work) {
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		for (int start = 0; start < max; start++) {
			
			for (int length = 1; length <= max-1; length++) {
				List<Integer> numberList = new LinkedList<Integer>();
				Iterator<Integer> it = work.iterator();
				for (int i = 0; i < start; i++) {
					if (it.hasNext()) {
						it.next();
					} else {
						// 最初から
						it = work.iterator();
					}
				}
				
				for (int index = 0; index < length ; index++) {
					if (it.hasNext()) {
						numberList.add(it.next());
					} else {
						it = work.iterator();
						numberList.add(it.next());
					}
				}
				result.add(numberList);
			}
		}
		
		// 先頭から末までの1パターンを追加
		List<Integer> numberList = new LinkedList<Integer>();
		for (Iterator<Integer> it = work.iterator(); it.hasNext();) {
			numberList.add(it.next());
		}
		result.add(numberList);
		
		return result;
	}
	
	/**
	 * 数列 C(max,n)のn=1～maxまでのすべてのパターンを求める
	 * @param max
	 * @return
	 */
	public List<List<Integer>> combination(final int max) {
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		CompletionService<List<List<Integer>>> completion = 
	              new ExecutorCompletionService<List<List<Integer>>>(pool);
		for (int i = 1; i <= max; i++) {
			completion.submit(new Task(max, i));
		}
		
		for (int i = 1; i <= max; i++) {
			try {
				Future<List<List<Integer>>> future = completion.take();
				for (List<Integer> pattern : future.get()) {
					result.add(pattern);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		try {
			pool.shutdown();
			pool.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	/**
	 * maxからmax個取り出す場合の順列をすべて求める
	 * @param max
	 * @param circlNumberList
	 * @return
	 */
	private List<List<Integer>> numberCombination(int max) {
		List<Integer> circlNumberList = new LinkedList<Integer>();
		for (int i = 0; i <= max; i++) {
			circlNumberList.add(i);
		}
		
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		_combination(0, max, max, circlNumberList, result);
		return result;
	}
	
	private void _combination(int startIndex, int max, int count, List<Integer> work, List<List<Integer>> result)
	{
		if(startIndex==count){
			List<Integer> numberList = new LinkedList<Integer>();
			for (int i = 1; i <= count; i++){
				numberList.add(work.get(i));
			}
			result.add(numberList);
		}
		else {
			//桁数を増す
			int w = work.get(startIndex+1);
			for (int i = startIndex + 1; i <= max; i++){
				work.set(startIndex+1, work.get(i));//固定する数
				work.set(i, w);
				_combination(startIndex+1, max, count, work, result);//順列の場合
//				if (work.get(startIndex) < work.get(startIndex+1)) {
//					_combination(startIndex+1, max, count, work, result);//組み合わせの場合
//				}
				work.set(i, work.get(startIndex+1));
			}
			work.set(startIndex+1, w);
		}
	}
	

	private String resultToString(State state) {
		StringBuilder sb = new StringBuilder();
		sb.append("結果を表示します").append(StringUtil.SEP)
		  .append("引数L: ").append(length).append(StringUtil.SEP)
		  .append("解の個数: ").append(result.size()).append("個").append(StringUtil.SEP);
		
		if (state.isError()) {
			sb.append("解答不能です").append(StringUtil.SEP)
			  .append(StringUtil.SEP)
			  .append(state.message());
		} else {
			sb.append(StringUtil.SEP);
			for (int i = 0; i < result.size(); i++) {
				sb.append("解").append(i+1).append(StringUtil.SEP)
				  .append(result.get(i).toString()).append(StringUtil.SEP)
				  .append(StringUtil.SEP);
			}
		}
		return sb.toString();
	}
	
}
