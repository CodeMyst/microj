# microj ![build badge](https://github.com/CodeMyst/microj/actions/workflows/gradle.yml/badge.svg)

Compiler for a Java-like language. Made for the Compiler Construction course, at the Faculty of Sciences, Novi Sad.

Example of micro java:

```java
program P
	final int size = 10;
	class Table {
		int[] pos;
		int[] neg;
	}
	Table val;
{
	void main()
		int x, i;
	{ //---------- Initialize val
		val = new Table;
		val.pos = new int[size]; val.neg = new int[size];
		i = 0;
		while (i < size) {
			val.pos[i] = 0; val.neg[i] = 0;
			i++;
		}
	//---------- Read values
		read(x);
		while (x != 0) {
			if (0 <= x && x < size) {
				val.pos[x]++;
			} else if (-size < x && x < 0) {
				val.neg[-x]++;
			}
			read(x);
		}
	}
}
```
