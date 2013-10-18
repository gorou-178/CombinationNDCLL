package cnd;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import util.StringUtil;

public class CndResult {
	private List<Integer> numberCirclList;
	private List<Integer> totalNumberList;
	private List<String> patternList;
	
	public CndResult(List<Integer> numberCirclList, List<Integer> totalNumberList, List<String> patternList) {
		this.numberCirclList = numberCirclList;
		this.totalNumberList = totalNumberList;
		this.patternList = patternList;
	}

	public List<Integer> getNumberCirclList() {
		return numberCirclList;
	}

	public List<Integer> getTotalNumberList() {
		return totalNumberList;
	}

	public List<String> getPatternList() {
		return patternList;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("循環リストX: ").append(StringUtil.SEP);
		
		int count = 1;
		for (Iterator<Integer> it = numberCirclList.iterator(); it.hasNext(); ++count) {
			Integer number = it.next();
			if ((count % 10) == 0) {
				sb.append(StringUtil.SEP);
			}
			
			sb.append(number).append("->");
		}
		sb.append("先頭").append(StringUtil.SEP)
		  .append(StringUtil.SEP)
		  .append("集合N(X): ").append(StringUtil.SEP)
		  .append("{").append(StringUtil.SEP)
		  .append("    ");
		
		
		count = 1;
		for (Iterator<Integer> it = totalNumberList.iterator(); it.hasNext();) {
			Integer number = it.next();
			if ((count % 10) == 0) {
				sb.append(StringUtil.SEP)
				  .append("    ");
			}
			
			sb.append(number);
			if (it.hasNext()) {
				sb.append(",");
			}
		}
		sb.append(StringUtil.SEP)
		  .append("}").append(StringUtil.SEP)
		  .append(StringUtil.SEP);
		
		
		sb.append("パターン: ").append(StringUtil.SEP);
		for (String pattern : patternList) {
			sb.append(pattern).append(StringUtil.SEP);
		}
		
		return sb.toString();
	}
	
}
