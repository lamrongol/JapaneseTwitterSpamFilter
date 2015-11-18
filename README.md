このプロジェクトをクローンしてbuild.sbtに以下のように記述するとインポートできます。
	lazy val spamFilter = RootProject(file("C:\\workspaces\\TwitterAnalysis\\SpamFilter"))
	
	val main = Project(id = "YOUR_PROJECT", base =file(".")).dependsOn(spamFilter)

クローンした場所は自分の環境に合わせ、YOUR_PROJECTの部分は自分のプロジェクトのsbtファイルに記載されているnameに変えてください。
	
自分で訓練データを用意せず私が作成したものを用いる場合以下のようにすればすぐ使えます。

	val japaneseSpamFilter = JapaneseSpamFilter.loadDefault
	japaneseSpamFilter.isSpam(status)//twitter4jで取得したstatus

詳しい解説・使い方は以下のページを見てください。
http://qiita.com/lamrongol/items/8c670351ef2b96d4fbb6

