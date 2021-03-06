
package liquibase.structure.core;

public class View extends Relation {

    public View() {}

    @Override
    public Relation setSchema(Schema schema) {
        return super.setSchema(schema);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public String getDefinition() {
        return getAttribute("definition", String.class);
    }

    public void setDefinition(String definition) {
        this.setAttribute("definition", definition);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        View that = (View) o;

        return getName().equalsIgnoreCase(that.getName());

    }

    @Override
    public int hashCode() {
        return getName().toUpperCase().hashCode();
    }

    @Override
    public String toString() {
        StringBuilder viewBuilder = new StringBuilder(getName());
        viewBuilder.append(" (");
        for (int i = 0; i < getColumns().size(); i++) {
            viewBuilder.append(i > 0 ? "," : "").append(getColumns().get(i));
        }
        viewBuilder.append(")");
        return viewBuilder.toString();
    }

    @Override
    public View setName(String name) {
        return (View) super.setName(name);
    }

}
