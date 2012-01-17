package jr.library.model;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class SuperEntity{
	@Id
	@GeneratedValue(strategy=GenerationType.TABLE)
	private Long id;
	public abstract void initializeLazyFields();
	public Long id() { return id; }
	public void id_$eq(Long id) {this.id = id;}

	public Long getId() {
		return id;
	}

	public void setId(Long _id) {
		this.id = _id;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final SuperEntity other = (SuperEntity) obj;
		if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 83 * hash + (this.id != null ? this.id.hashCode() : 0);
		return hash;
	}

}
