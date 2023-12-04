public class Element implements Comparable<Element> {
        int array, index;
        String value;

        public Element(int array, int index, String value) {
            this.array = array;
            this.index = index;
            this.value = value;
        }

        @Override
        public int compareTo(Element other) {
            return this.value.compareTo(other.value);
        }

        public int getArray() {
            return this.array;
        }

        public int getIndex() {
            return this.index;
        }

        public String getValue() {
            return this.value;
        }
    }
