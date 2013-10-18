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
	 * ���U�̏��
	 * ���z�̓v���Z�b�T�����Ǝv��
	 */
	private final static int THREAD_MAX_SIZE = 10;
//	private final static int THREAD_MAX_SIZE = Runtime.getRuntime().availableProcessors();
	
	/** ����L�̍ŏ��l **/
	private final static int LENGTH_MIN = 2;
	/** ����L�̍ő�l(�ƒf�ƕΌ�) **/
	private final static int LENGTH_MAX = 10000;
	
	
	enum State {
		Success(1, "����I��"),
		NoAnser(0, "����I��(���Ȃ�)"),
		LengthMinError(-1, "�����̒l�����������܂��B" + LENGTH_MIN + "���ŏ��l�ł��B"),
		LengthMaxError(-2, "�����̒l���傫�����܂��B" + LENGTH_MAX + "�܂Ŏw��\�ł��B")
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
				//�����𑝂�
				int w = work.get(startIndex+1);
				for (int i = startIndex + 1; i <= max; i++){
					work.set(startIndex+1, work.get(i));//�Œ肷�鐔
					work.set(i, w);
					_combination(startIndex+1, max, count, work, result);//����̏ꍇ
//					if (work.get(startIndex) < work.get(startIndex+1)) {
//						_combination(startIndex+1, max, count, work, result);//�g�ݍ��킹�̏ꍇ
//					}
					work.set(i, work.get(startIndex+1));
				}
				work.set(startIndex+1, w);
			}
		}
	}
	
	/** ����L **/
	private int length;
	/** �g�ݍ��킹�̃p�^�[���� **/
	private int totalPattern;
	/** ���̃��X�g **/
	private List<CndResult> result;
	/** �^�X�N�Ǘ� **/
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
		
		// �p�^�[���������߂�
		totalPattern = calcTotalPattern();
		
		// �z���X�g�̃p�^�[�����擾
		// 1. �擪�͕K��1
		// 2. 2�Ԗڂ̗v�f < �����̗v�f
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
		
		// �������킹�̃p�^�[�������ׂċ��߂�
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
			
			// �����`�F�b�N
			if (totalPattern != patternList.size()) {
				patternList.add("���p�^�[�����ƈ�v���܂���ł���");
			}
			
			result.add(new CndResult(numberCirclList, totalNumberList, patternList));
		}
		
		return resultToString(State.Success);
	}
	
	private State checkError() {
		// 2��菬�����ꍇ�G���[�Ƃ���
		if (length < LENGTH_MIN) {
			return State.LengthMinError;
		}

		// �������Ԃ⃁�����̗��R�łƂ肠�����ő�l��݂��Ă���
		if (length >= LENGTH_MAX) {
			return State.LengthMaxError;
		}
		
		return State.Success;
	}
	
	/**
	 * �p�^�[���������߂�
	 * n(L) = L(L - 1) + 1
	 * @return
	 */
	private int calcTotalPattern() {
		return (length * (length - 1)) + 1;
	}
	
	/**
	 * �A�Ԃ̃��X�g���o��
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
	 * �z���X�g�̐؂���ʒu���Œ肵�āA1�`max���ꂼ��̒����Ő؂���
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
						// �ŏ�����
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
		
		// �擪���疖�܂ł�1�p�^�[����ǉ�
		List<Integer> numberList = new LinkedList<Integer>();
		for (Iterator<Integer> it = work.iterator(); it.hasNext();) {
			numberList.add(it.next());
		}
		result.add(numberList);
		
		return result;
	}
	
	/**
	 * ���� C(max,n)��n=1�`max�܂ł̂��ׂẴp�^�[�������߂�
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
	 * max����max���o���ꍇ�̏�������ׂċ��߂�
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
			//�����𑝂�
			int w = work.get(startIndex+1);
			for (int i = startIndex + 1; i <= max; i++){
				work.set(startIndex+1, work.get(i));//�Œ肷�鐔
				work.set(i, w);
				_combination(startIndex+1, max, count, work, result);//����̏ꍇ
//				if (work.get(startIndex) < work.get(startIndex+1)) {
//					_combination(startIndex+1, max, count, work, result);//�g�ݍ��킹�̏ꍇ
//				}
				work.set(i, work.get(startIndex+1));
			}
			work.set(startIndex+1, w);
		}
	}
	

	private String resultToString(State state) {
		StringBuilder sb = new StringBuilder();
		sb.append("���ʂ�\�����܂�").append(StringUtil.SEP)
		  .append("����L: ").append(length).append(StringUtil.SEP)
		  .append("���̌�: ").append(result.size()).append("��").append(StringUtil.SEP);
		
		if (state.isError()) {
			sb.append("�𓚕s�\�ł�").append(StringUtil.SEP)
			  .append(StringUtil.SEP)
			  .append(state.message());
		} else {
			sb.append(StringUtil.SEP);
			for (int i = 0; i < result.size(); i++) {
				sb.append("��").append(i+1).append(StringUtil.SEP)
				  .append(result.get(i).toString()).append(StringUtil.SEP)
				  .append(StringUtil.SEP);
			}
		}
		return sb.toString();
	}
	
}
