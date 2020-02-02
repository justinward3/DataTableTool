import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class Node<T> {

    private T data = null;

    private List<Node<T>> children = new ArrayList<>();

    private Node<T> parent = null;

    public Node(T data) {
        this.data = data;
    }

    public Node<T> addChild(Node<T> child) {
        child.setParent(this);
        this.children.add(child);
        return child;
    }

    public void addChild(Node<T> root, long ID, Node<T> child) {
        if (root.getData() != null) {
            JSONObject temp = (JSONObject) root.getData();
            if ((long) temp.get("memberId") == ID) {
                root.addChild(child);
                return;
            }
            else {
                root.getChildren().forEach(each -> addChild(each, ID, child));
            }
        }
        else{
            root.setData(child.getData());
            root.setParent(child.getParent());
        }
    }

    public void Print(Boolean Levels, JSONArray jsonMatch, BufferedWriter bf, Node<T> root, String append, String language, int level, HashMap<Long,String> FootNoteMap) {
        if (root.getData() != null) {
            level+=1;
            JSONObject temp = (JSONObject) root.getData();
            String UOM = "";
            for(Object i : jsonMatch){
                long code1 = (long)(((JSONObject)i).get("memberUomCode"));
                long code2 = -1;
                if(temp.get("memberUomCode")!=null) {
                    code2 = (long) (temp.get("memberUomCode"));
                }
                if(code1==code2){
                    UOM = " UOM : "+(String)((JSONObject)i).get("memberUom"+language);
                }
            }
            try {
                bf.newLine();
                if(Levels) {
                    bf.write(append + "L" + level);
                    bf.newLine();
                }
                if (FootNoteMap.containsKey((long) temp.get("memberId"))) {
                    bf.write(append + temp.get("memberName" + language) + "  Footnote : " + FootNoteMap.get((long) temp.get("memberId"))+UOM);
                }
                else{
                    bf.write(append + temp.get("memberName" + language)+ UOM);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            int finalLevel = level;
            root.getChildren().forEach(each -> Print(Levels,jsonMatch,bf,each, append + "\t", language, finalLevel,FootNoteMap));
        }
        try {
            bf.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void Print(Boolean Levels, JSONArray jsonMatch, BufferedWriter bf, Node<T> root, String append, String language, int level) {
        if (root.getData() != null) {
            level+=1;
            JSONObject temp = (JSONObject) root.getData();
            String UOM = "";
            for(Object i : jsonMatch){
                long code1 = (long)(((JSONObject)i).get("memberUomCode"));
                long code2 = -1;
                if(temp.get("memberUomCode")!=null) {
                    code2 = (long) (temp.get("memberUomCode"));
                }
                if(code1==code2){
                    UOM = " UOM : "+(String)((JSONObject)i).get("memberUom"+language);
                }
            }
            try {
                bf.newLine();
                if(Levels) {
                    bf.write(append + "L" + level);
                    bf.newLine();
                }
                bf.write(append + temp.get("memberName" + language) + UOM);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int finalLevel = level;
            root.getChildren().forEach(each -> Print(Levels,jsonMatch,bf,each, append + "\t", language, finalLevel));
        }
        try {
            bf.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Node<T> Search(Node<T> root, String NodeName, String language) {
        if (((JSONObject)root.getData()).get("memberNameEn") == NodeName) {
            return root;
        }
        else {
            for(Node<T> child : root.getChildren()){
                return Search(child, NodeName, language);

            }
        }
        return null;
    }

    public ArrayList<Node<T>> LevelSearch(Node<T> root, String NodeName, String language) {
        if (((JSONObject)root.getData()).get("memberName" + language) == NodeName) {
            if(root.getParent().getParent() != null){
                return (ArrayList<Node<T>>)root.getParent().getParent().getChildren();
            }
            else{
                return null;
            }
        }
        else {
            for(Node<T> child : root.getChildren()){
                return LevelSearch(child, NodeName, language);

            }
        }
        return null;
    }

    public void addChildren(List<Node<T>> children) {
        children.forEach(each -> each.setParent(this));
        this.children.addAll(children);
    }

    public List<Node<T>> getChildren() {
        return children;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    private void setParent(Node<T> parent) {
        this.parent = parent;
    }

    public Node<T> getParent() {
        return parent;
    }

}