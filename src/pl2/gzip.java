/* Author: Rui Pedro Paiva
Teoria da Informa��o, LEI, 2006/2007*/
package pl2;
 
import java.io.*;
import java.util.*;
 
//class principal para leitura de um ficheiro gzip
//M�todos:
//gzip(String fileName) throws IOException --> construtor
//int getHeader() throws IOException --> l� o cabe�alho do ficheiro para um objecto da class gzipHeader
//void closeFiles() throws IOException --> fecha os ficheiros
//String bits2String(byte b) --> converte um byte para uma string com a sua representa��o bin�ria
public class gzip
{
    static gzipHeader gzh;
    static String gzFile;
    static long fileSize;
    static long origFileSize;
    static int numBlocks = 0;
    static HuffmanTree huffmantree = new HuffmanTree();
    static RandomAccessFile is;
    static int rb = 0, needBits = 0, availBits = 0;    
  
    static int len[] = {
        11, 13, 15, 17, 19, 23, 27,
        31, 35, 43, 51, 59, 67, 83,
        99, 115, 131, 163, 195, 227
    };
  static int dist[] = {
    5, 7, 9, 13, 17, 25, 33, 49,
    65, 97, 129, 193, 257, 385,
    513, 769, 1025, 1537, 2049,
    3073, 4097, 6145, 8193,
    12289, 16385, 24577
  };

    //fun��o principal, a qual gere todo o processo de descompacta��o
    public static void main (String args[])
    {          
        //--- obter ficheiro a descompactar
        String fileName = "FAQ.txt.gz";
        /*if (args.length != 1)
        {
            System.out.println("Linha de comando inv�lida!!!");
            return;
        }
        String fileName = args[0];*/           
               
        //--- processar ficheiro
        try
        {
            gzip gz = new gzip(fileName);
           
            //ler tamanho do ficheiro original e definir Vector com s�mbolos
            origFileSize = getOrigFileSize();
           
            //--- ler cabe�alho
            int erro = getHeader();
            if (erro != 0)
            {
                System.out.println ("Formato inv�lido!!!");
                return;
            }
            //else             
            //  System.out.println(gzh.fName);
           
           
            //--- Para todos os blocos encontrados
            int BFINAL;
           
            do
            {              
                //--- ler o block header: primeiro byte depois do cabe�alho do ficheiro
                needBits = 3;
                             
                if (availBits < needBits)
                {
                    rb = is.readUnsignedByte() << availBits | rb;
                    availBits += 8;
                }
               
                //obter BFINAL
                //ver se � o �ltimo bloco
                           
 
                BFINAL = (byte) (rb & 0x01); //primeiro bit � o menos significativo
                //System.out.println("BFINAL = " + BFINAL);
                               
                //analisar block header e ver se � huffman din�mico                
                if (!isDynamicHuffman(rb))  //ignorar bloco se n�o for Huffman din�mico
                    continue;
                //descartar os 3 bits correspondentes ao tipo de bloco
                rb = rb >> 3;
                availBits -= 3;
                
                // Ex1
                int hlit = readBits(5), hdist = readBits(5), hclen = readBits(4);
                
                //Ex2
                int[] order = {16,17,18,0,8,7,9,6,10,5,11,4,12,3,13,2,14,1,15};
                ArrayList<Integer> bits = new ArrayList<>();
                
                for (int i = 0; i < 19; i++) {
                    bits.add(0);
                }
                
                for (int i = 0; i < (hclen+4); i++) {
                    bits.set(order[i],readBits(3));
                }
                
                //Ex3
                ArrayList<String> huffmanCodes = lengthtoHuffman(bits);
                HuffmanTree lengthtree = createHuffmanTree(huffmanCodes);
                    
                //Ex4
                ArrayList<Integer> literalCodes = Codes(hlit+257,lengthtree);

                //Ex5
                ArrayList<Integer> distanceCodes = Codes(hdist+1,lengthtree);
                
                //Ex6
                ArrayList<String> literalHuffman = lengthtoHuffman(literalCodes);
                ArrayList<String> distanceHuffman = lengthtoHuffman(distanceCodes);

                HuffmanTree literalTree =   createHuffmanTree(literalHuffman);
                HuffmanTree distanceTree =   createHuffmanTree(distanceHuffman);
                
                //Ex7
                ArrayList<Integer> output = deflate(literalTree,distanceTree);
                                
                //Ex8
                String st = "";
                for (int i = 0; i < output.size(); i++) {
                    int p = output.get(i);                                    
                    st = st.concat(String.valueOf((char)p));
                    
                }
                
                String name = gzh.fName;
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(name)))) {
                    writer.write(st);
                    writer.close();
                    System.out.println("File created with name: " + name);
                } 
                catch (IOException ex) {
                    System.out.println("Error Creating file");
                } 
               
                //actualizar n�mero de blocos analisados*/
                numBlocks++;               
            }while(BFINAL == 0);
                       
 
            is.close();
            System.out.println("End: " + numBlocks + " bloco(s) analisado(s).");
        }
        catch (IOException erro)
        {
            System.out.println("Erro ao usar o ficheiro!!!");
            System.out.println(erro);
        }
    }
 
   public static ArrayList<Integer> deflate(HuffmanTree literalTree, HuffmanTree distanceTree) throws IOException{
        ArrayList<Integer> values = new ArrayList<>();
        boolean endBlock = false;
        int bit,pos;
        while(!endBlock){           
           literalTree.resetCurNode();
           do{
                    bit = readBits(1);
                    if(bit==1)
                        pos = literalTree.nextNode('1');
                    else
                        pos = literalTree.nextNode('0');
            }while(pos==-2);
           if(pos < 256){
               values.add(pos);
           }
           else if(pos == 256){
               break;               
           }
           else{
               int length;
               if(pos<265){
                   length = pos-254;
               }
               
                else if(pos<285){
                    int extra = readBits((pos-261)/4);
                    length = extra + len[pos-265];                                      
                }
                else{
                    length = 258;
                }
               
                //Distances
                int pos2=0;                  
                int bit2 ; 
                distanceTree.resetCurNode();
                do{
                    bit2 = readBits(1);
                    if(bit2==1)
                        pos2 = distanceTree.nextNode('1');
                    else
                        pos2 = distanceTree.nextNode('0');
                }while(pos2 == -2);

                if(pos2>3){
                    int extra = readBits((pos2-2)/2);
                    pos2 = extra + dist[pos2-4]-1;                   
                }              
                
                while(length>0){
                    int size = values.size()-1;
                    values.add(values.get(size-pos2));
                    length--;
               }
            }            
       }
       
       return values;
   }
    //Construtor: recebe nome do ficheiro a descompactar e cria File Streams
    gzip(String fileName) throws IOException
    {
        gzFile = fileName;
        is = new RandomAccessFile(fileName, "r");
        fileSize = is.length();
    }
        public static ArrayList<Integer> Codes(int num,HuffmanTree tree) throws IOException {
            int pos=0;
            ArrayList<Integer> indexs = new ArrayList<>();
            int lastindex=0;
            int bit;
            while (indexs.size() <num) {
                tree.resetCurNode();
                do{
                    bit = readBits(1);
                    if(bit==1)
                        pos = tree.nextNode('1');
                    else
                        pos = tree.nextNode('0');

                }while(pos==-2);
                if(pos>=0){
                    switch(pos){
                        //Repetir pos anterior
                        case 16:
                           bit = readBits(2);
                            for (int j = 0; j < bit+3; j++) {
                                indexs.add(lastindex);
                            }
                            break;
                        //Add 3 zeros
                        case 17:
                           bit = readBits(3);
                            for (int j = 0; j < bit+3; j++) {
                                indexs.add(0);
                            }
                            break;
                        //Add 7 zeros  
                        case 18:
                           bit = readBits(7);

                            for (int j = 0; j < bit+11; j++) {
                                indexs.add(0);
                            }
                            break;
                        default:       
                            indexs.add(pos);
                            lastindex = pos;
                            break;
                    }
                }
              
            }
         
             return indexs;
        }
        public static int maskBits(int numBits) {
            return (int) (Math.pow(2,numBits)-1);
        }
         public static int readBits(int numBits) throws IOException {
            int bit;
            if(availBits<numBits){
                bit = rb;
                int numbits2 = numBits-availBits;
                do{
                    rb = (is.readUnsignedByte()<<availBits) | bit;
                    bit = rb;
                    availBits+=8;
                    numbits2-=8;
                }while(numbits2>0);  
            }
            bit = (rb & maskBits(numBits));
            rb = rb >> numBits;
            availBits-=numBits;
           
            return bit;
        }
        public static HuffmanTree createHuffmanTree(ArrayList<String> huffmanCodes){
            HuffmanTree tree = new HuffmanTree();
            for (int i = 0; i < huffmanCodes.size(); i++) {
                if(!huffmanCodes.get(i).equals("")){
                    tree.addNode(huffmanCodes.get(i), i, false);
                }
            }
            return tree;
        }        
        public static ArrayList<String> lengthtoHuffman(ArrayList<Integer> comp){
            int max = 0;
            for (int i = 0; i <comp.size() ; i++) {
                if(comp.get(i)>max)
                    max = comp.get(i);
            }
            int[] b_count = new int[max+1];
            ArrayList<String> huffmancodes = new ArrayList<>();
            int i;
            for (i = 0; i < comp.size(); i++) {
                b_count[comp.get(i)]++;                
            }
            int[] next_code = new int[max+1];
           
            b_count[0] = 0;
            int code = 0;
            for(i = 1 ; i <=max;i++){
                code = (code + b_count[i-1]) <<1;
                next_code[i] = code;
            }
           
            int[] huffmanCode = new int[comp.size()];
            for (int j = 0; j < comp.size(); j++) {
                huffmanCode[j] = (int) (next_code[comp.get(j)]);
                next_code[comp.get(j)]++;
                huffmancodes.add(bits2String(huffmanCode[j],comp.get(j)));                                
            }          
            return huffmancodes;
        }
    //Obt�m tamanho do ficheiro original
    public static long getOrigFileSize() throws IOException
    {
        //salvaguarda posi��o actual do ficheiro
        long fp = is.getFilePointer();
       
        //�ltimos 4 bytes = ISIZE;
        is.seek(fileSize-4);
       
        //determina ISIZE (s� correcto se cabe em 32 bits)
        long sz = 0;
        sz = is.readUnsignedByte();
        for (int i = 0; i <= 2; i++)
            sz = (is.readUnsignedByte() << 8*(i+1)) + sz;          
       
        //restaura file pointer
        is.seek(fp);
       
        return sz;     
    }
       
 
    //L� o cabe�alho do ficheiro gzip: devolve erro se o formato for inv�lido
    public static int getHeader() throws IOException  //obt�m cabe�alho
    {
        gzh = new gzipHeader();
       
        int erro = gzh.read(is);
        if (erro != 0) return erro; //formato inv�lido       
       
        return 0;
    }
       
   
    //Analisa block header e v� se � huffman din�mico
    public static boolean isDynamicHuffman(int k)
    {              
        byte BTYPE = (byte) ((k & 0x06) >> 1);
                       
        if (BTYPE == 0) //--> sem compress�o
        {
            System.out.println("Ignorando bloco: sem compacta��o!!!");
            return false;
        }
        else if (BTYPE == 1)
        {
            System.out.println("Ignorando bloco: compactado com Huffman fixo!!!");
            return false;
        }
        else if (BTYPE == 3)
        {
            System.out.println("Ignorando bloco: BTYPE = reservado!!!");
            return false;
        }
        else
            return true;
       
    }
   
   
    //Converte um byte para uma string com a sua representa��o bin�ria
    public static String bits2String(int b,int length)
    {
        String strBits = "";
        int mask = 0x01;  //get LSbit
       
        for (int bit, i = 1; i <= length; i++)
        {
            bit = (int)(b & mask);
            strBits = bit + strBits; //add bit to the left, since LSb first
            b >>= 1;
        }
        return strBits;    
    }
}