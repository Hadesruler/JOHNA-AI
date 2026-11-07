package com.johna.assistant
fun main(){
 val wakeInputs= listOf(
  "Hey Johna" to "", "Johna άνοιξε καιρό" to "ανοιξε καιρο", "Τζόνα άνοιξε Gmail" to "ανοιξε gmail",
  "Hey τζόνα, πήγαινε στην αρχική" to "πηγαινε στην αρχικη", "Άσχετη συζήτηση" to null
 )
 for((q,wanted) in wakeInputs){ val actual=WakePhraseParser.strip(q)
  check(actual==wanted){"Wake '$q': expected $wanted, found $actual"};println("PASS wake: $q -> $actual") }
 val cases=listOf(
  Triple("volume down 2", 7, 5),Triple("ανέβασε ένταση δύο",4,6),Triple("βάλε ένταση 7",3,7),
  Triple("σίγαση",8,0),Triple("τερμάτισε τη φωνή",8,10),Triple("volume up 9",6,10),
  Triple("χαμήλωσε τη φωνή",4,3),Triple("unmute",0,5))
 for((q,current,wanted) in cases) {val command=LocalVolumeParser.parse(q) ?: error("Parser returned null for $q")
  val actual=LocalVolumeParser.target(current,command)
  check(actual==wanted){"Volume '$q': expected $wanted found $actual"};println("PASS volume: $q -> $actual")}
 check(LocalVolumeParser.parse("άνοιξε τον καιρό")==null)
 println("PASS router: ${wakeInputs.size+cases.size+1} tests")
}
