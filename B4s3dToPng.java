import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

class B4s3dToPng{
    static CoreTwo[] cores;
    public static void main(String[] args) throws Exception{
        String imgPath = "test_144_b4s3d"; //select image to change
        BufferedImage img = ImageIO.read(new File(imgPath+".png"));

        int coresNum = Runtime.getRuntime().availableProcessors();
        int batchSize = (int)Math.ceil(img.getWidth()*img.getHeight() / coresNum); // to not lose last elements from a batch

        cores = new CoreTwo[coresNum];

        System.err.println("BatchSize: "+batchSize);

        cores[0] = new CoreTwo(0, img, batchSize, img.getWidth(), img.getHeight()); // first out of loop to not rewrite the static BufferedImage
        for (int id = 1; id < coresNum; id++) {
            cores[id] = new CoreTwo(id);
        }

        //run threads
        for (int i = 0; i < cores.length; i++) {
            cores[i].run();
        }

        //join all Threads to save file
        for (int i = 0; i < cores.length; i++) {
            if(cores[i] != null){
                cores[i].join();
            }
        }

        ImageIO.write(img, "png", new File(imgPath+"_fromB4s3d.png"));
    }
}

class CoreTwo extends Thread{
    final int id;
    static BufferedImage img;
    static int[] memory;
    static double base = 1.02196827096302; //255sqrt(255)

    //unnecessary for this i think
    static synchronized void setRGB(int x, int y, int rgb){
        img.setRGB(x, y, rgb);
    }

    //first core constructor
    public CoreTwo(int id, BufferedImage image, int... mem){
        this.id = id;
        img = image;
        memory = mem;
    }

    //other cores constructor
    public CoreTwo(int id){
        this.id = id;
    }

    @Override
    public void run(){
        int end = Math.min(memory[0]*(id+1), memory[1]*memory[2]-1); //out of bounds check (only for last core relevant)
        for (int i = id*memory[0]; i < end; i++) {
            //System.err.println("Core "+id+": iteration "+i); //DEBUG
            int x = i % memory[1];
            int y = i / memory[1];
            setRGB(x, y, convert(img.getRGB(x, y)));
        }
    }

    //just for readability not inline
    private int convert(int argb){

        //unpack int
        int a = (argb >> 24) & 0xff;
        int r = (argb >> 16) & 0xff;
        int g = (argb >> 8) & 0xff;
        int b = argb & 0xff;

        r = (int) Math.pow(base, r);
        g = (int) Math.pow(base, g);
        b = (int) Math.pow(base, b);

        return (a << 24) | (r << 16) | (g << 8) | b; //ARGB
    }
}