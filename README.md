# 双方向循環リストのノードの組み合わせ問題
N(X) = {1, ・・・}

集合N(X)の個数が 

	n(L) = L(L - 1) + 1
	
である場合のN(X)の組み合わせすべてを出力

解がない場合はその旨を出力する

## 動作環境
	Java version: 6以上

## Usage
	git clone https://github.com/gurimmer/NumberAutoAnser.git
	cd CombinationNDCLL
	java -Dfile.encoding=utf-8 -jar CombinationNDCLL.jar <整数>

### 定義
- L: プログラム実行時に引数として渡された整数。
- n(L): 引数Lが渡された場合のノードの組み合わせの総数。n(L)=L(L-1)+1で求められる。
- N(X): n(L)を満たすL個の集合。ノードの組み合わせの和の数集合。
- X: n(L)個の数集合N(X)を満たす長さLの循環リスト

## 要点
- Lは1以上の整数
- Xの先頭ノードは1。2番目は、末尾ノードより小さいこと
- Lの数が大きいと実行オーダーが極端に上がるためタスク化して分散処理させる
- たぶん組み合わせをどうプログラムで求めるかという所
- ノードの数字のルールのが無いので、Lを最大数と勝手に決めた
- 循環リストだが双方向循環リンクリストっぽい

出力は以下のようにしたい

	結果を表示します
	引数: L
	解の個数: 1個

	解1
	循環リストX: 1→2→3→・・・・8→先頭
	集合N(X): {1,2,3・・・・・10}
	パターン:
	1
	2
	1+2=3
	・
	・
	・
		
	解2
	循環リストX: 1→2→3→・・・・8→先頭
	集合N(X): {1,2,3・・・・・10}
	パターン:
	1
	2
	1+2=3
	・
	・
	・

解がない場合

	結果を表示します
	引数: L
	解の個数: 0個
	解答不能です(引数には整数を指定してください)

こんなかんじ